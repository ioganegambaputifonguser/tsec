package tsec

import java.time.Instant
import java.util.UUID

import cats.data.{Kleisli, OptionT}
import cats.implicits._
import cats.{Applicative, Monad}
import io.circe._
import org.http4s._
import org.http4s.headers.{Authorization, Cookie => C}
import org.http4s.server.Middleware
import tsec.authorization.{Authorization => TAuth}

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

package object authentication {

  trait BackingStore[F[_], I, V] {
    def put(elem: V): F[V]

    def get(id: I): OptionT[F, V]

    def update(v: V): F[V]

    def delete(id: I): F[Unit]
  }

  type AuthExtractorService[F[_], Ident, Auth] = Kleisli[OptionT[F, ?], Request[F], SecuredRequest[F, Ident, Auth]]

  /** Inspired from the Silhouette `SecuredRequest`
    *
    */
  final case class SecuredRequest[F[_], Identity, Auth](request: Request[F], identity: Identity, authenticator: Auth)

  object asAuthed {

    /** Matcher for the http4s dsl
      * @param ar
      * @tparam F
      * @tparam A
      * @tparam I
      * @return
      */
    def unapply[F[_], I, A](ar: SecuredRequest[F, I, A]): Option[(Request[F], I)] =
      Some(ar.request -> ar.identity)
  }

  type TSecMiddleware[F[_], I, A] =
    Middleware[OptionT[F, ?], SecuredRequest[F, I, A], Response[F], Request[F], Response[F]]

  object TSecMiddleware {
    def apply[F[_]: Monad, Ident, Auth](
        authedStuff: Kleisli[OptionT[F, ?], Request[F], SecuredRequest[F, Ident, Auth]],
        onNotAuthenticated: Request[F] => F[Response[F]]
    ): TSecMiddleware[F, Ident, Auth] =
      service => {
        Kleisli { r: Request[F] =>
          OptionT.liftF(
            authedStuff
              .run(r)
              .flatMap(service.mapF(o => OptionT.liftF(o.getOrElse(Response[F](Status.NotFound)))).run)
              .getOrElseF(onNotAuthenticated(r))
          )
        }
      }
  }

  // The parameter types of TSecAuthService are reversed from what
  // we'd expect. This is a workaround to ensure partial unification
  // is triggered.  See https://github.com/jmcardon/tsec/issues/88 for
  // more info.
  type TSecAuthService[Ident, A, F[_]] = Kleisli[OptionT[F, ?], SecuredRequest[F, Ident, A], Response[F]]

  object TSecAuthService {

    /** Lifts a partial function to an `TSecAuthedService`.  Responds with
      * [[org.http4s.Response.notFound]], which generates a 404, for any request
      * where `pf` is not defined.
      */
    def apply[A, I, F[_]](
        pf: PartialFunction[SecuredRequest[F, I, A], F[Response[F]]]
    )(implicit F: Monad[F]): TSecAuthService[I, A, F] =
      Kleisli(req => pf.andThen(OptionT.liftF(_)).applyOrElse(req, Function.const(OptionT.none)))

    def apply[A, I, F[_]](
        pf: PartialFunction[SecuredRequest[F, I, A], F[Response[F]]],
        andThen: (Response[F], A) => OptionT[F, Response[F]]
    )(implicit F: Monad[F]): TSecAuthService[I, A, F] =
      Kleisli(
        req =>
          pf.andThen(OptionT.liftF(_))
            .applyOrElse(req, Function.const(OptionT.none[F, Response[F]]))
            .flatMap(r => andThen(r, req.authenticator))
      )

    def withAuthorization[A, I, F[_]](auth: TAuth[F, I, A])(
        pf: PartialFunction[SecuredRequest[F, I, A], F[Response[F]]]
    )(implicit F: Monad[F]): TSecAuthService[I, A, F] =
      Kleisli { req: SecuredRequest[F, I, A] =>
        auth
          .isAuthorized(req)
          .flatMap(_ => pf.andThen(OptionT.liftF(_)).applyOrElse(req, Function.const(OptionT.none)))
      }

    def withAuthorization[A, I, F[_]](auth: TAuth[F, I, A])(
        pf: PartialFunction[SecuredRequest[F, I, A], F[Response[F]]],
        andThen: (Response[F], A) => OptionT[F, Response[F]]
    )(implicit F: Monad[F]): TSecAuthService[I, A, F] =
      Kleisli(
        req =>
          auth
            .isAuthorized(req)
            .flatMap(
              _ =>
                pf.andThen(OptionT.liftF(_))
                  .applyOrElse(req, Function.const(OptionT.none[F, Response[F]]))
                  .flatMap(r => andThen(r, req.authenticator))
          )
      )

    /** The empty service (all requests fallthrough).
      * @tparam F - Ignored
      * @tparam Ident - Ignored
      * @tparam A - Ignored
      * @return
      */
    def empty[A, Ident, F[_]: Applicative]: TSecAuthService[Ident, A, F] =
      Kleisli.liftF(OptionT.none)
  }

  /** Common cookie settings for cookie-based authenticators
    *
    * @param cookieName
    * @param secure
    * @param httpOnly
    * @param domain
    * @param path
    * @param extension
    */
  final case class TSecCookieSettings(
      cookieName: String = "tsec-auth-cookie",
      secure: Boolean,
      httpOnly: Boolean = true,
      domain: Option[String] = None,
      path: Option[String] = None,
      extension: Option[String] = None,
      expiryDuration: FiniteDuration,
      maxIdle: Option[FiniteDuration]
  )

  final case class TSecTokenSettings(
      expiryDuration: FiniteDuration,
      maxIdle: Option[FiniteDuration]
  )

  final case class TSecJWTSettings(
      headerName: String = "X-TSec-JWT",
      expiryDuration: FiniteDuration,
      maxIdle: Option[FiniteDuration]
  )

  def cookieFromRequest[F[_]: Monad](name: String, request: Request[F]): OptionT[F, Cookie] =
    OptionT.fromOption[F](C.from(request.headers).flatMap(_.values.find(_.name === name)))

  def unliftedCookieFromRequest[F[_]](name: String, request: Request[F]): Option[Cookie] =
    C.from(request.headers).flatMap(_.values.find(_.name === name))

  def extractBearerToken[F[_]: Monad](request: Request[F]): Option[String] =
    request.headers.get(Authorization).flatMap { t =>
      t.credentials match {
        case Credentials.Token(scheme, token) if scheme == AuthScheme.Bearer =>
          Some(token)
        case _ => None
      }
    }

  def buildBearerAuthHeader(content: String): Authorization =
    Authorization(Credentials.Token(AuthScheme.Bearer, content))

  private[tsec] implicit val InstantLongDecoder: Decoder[Instant] = new Decoder[Instant] {
    def apply(c: HCursor): Either[DecodingFailure, Instant] =
      c.value
        .as[Long]
        .flatMap(
          l =>
            Either
              .catchNonFatal(Instant.ofEpochSecond(l))
              .leftMap(_ => DecodingFailure("InvalidEpoch", Nil))
        )
  }

  private[tsec] implicit val InstantLongEncoder: Encoder[Instant] = new Encoder[Instant] {
    def apply(a: Instant): Json = Json.fromLong(a.getEpochSecond)
  }

  def uuidFromRaw[F[_]: Applicative](string: String): OptionT[F, UUID] =
    try OptionT.pure(UUID.fromString(string))
    catch {
      case NonFatal(e) => OptionT.none
    }
}
