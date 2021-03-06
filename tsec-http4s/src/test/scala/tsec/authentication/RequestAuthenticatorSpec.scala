package tsec.authentication

import cats.data.OptionT
import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import tsec.authorization.BasicRBAC

class RequestAuthenticatorSpec extends AuthenticatorSpec {

  def requestAuthTests[A](title: String, authSpec: AuthSpecTester[A]) {

    behavior of "SecuredRequests: " + title

    val dummyBob = DummyUser(0)

    val requestAuth = SecuredRequestHandler(authSpec.auth)

    //Add bob to the db
    authSpec.dummyStore.put(dummyBob).unsafeRunSync()

    val onlyAdmins = BasicRBAC[IO, DummyRole, DummyUser, A](DummyRole.Admin)
    val everyone   = BasicRBAC.all[IO, DummyRole, DummyUser, A]

    val testService: HttpService[IO] = requestAuth {
      case request @ GET -> Root / "api" asAuthed hi =>
        Ok(hi.asJson)
    }

    val adminService: HttpService[IO] = requestAuth.authorized(onlyAdmins) {
      case request @ GET -> Root / "api" asAuthed hi =>
        Ok(hi.asJson)
    }

    val everyoneService: HttpService[IO] = requestAuth.authorized(everyone) {
      case request @ GET -> Root / "api" asAuthed hi =>
        Ok(hi.asJson)
    }

    val insaneService: HttpService[IO] = requestAuth {
      case request @ GET -> Root / "api" asAuthed hi =>
        IO.raiseError(new IllegalArgumentException)
    }

    val customService: TSecAuthService[DummyUser, A, IO] = TSecAuthService {
      case request @ GET -> Root / "api" asAuthed hi =>
        Ok()
    }

    it should "TryExtractRaw properly" in {

      val response: OptionT[IO, Option[String]] = for {
        auth <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), auth)
      } yield authSpec.auth.extractRawOption(embedded)
      response
        .getOrElse(None)
        .unsafeRunSync()
        .isDefined mustBe true
    }

    it should "Return a proper deserialized user" in {

      val response: OptionT[IO, Response[IO]] = for {
        auth <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), auth)
        res <- testService(embedded)
      } yield res
      response
        .getOrElse(Response.notFound)
        .flatMap(_.attemptAs[Json].value.map(_.flatMap(_.as[DummyUser])))
        .unsafeRunSync() mustBe Right(dummyBob)
    }

    it should "fail on an expired token" in {
      val response: OptionT[IO, Response[IO]] = for {
        auth    <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        expired <- OptionT.liftF(authSpec.expireAuthenticator(auth))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), expired)
        res <- testService(embedded)
      } yield res
      response
        .getOrElse(Response.notFound)
        .map(_.status)
        .unsafeRunSync() mustBe Status.Unauthorized
    }

    it should "work on a renewed token" in {

      val response: OptionT[IO, Response[IO]] = for {
        auth    <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        expired <- OptionT.liftF(authSpec.expireAuthenticator(auth))
        renewed <- OptionT.liftF(authSpec.auth.renew(expired))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), renewed)
        res <- testService(embedded)
      } yield res
      response
        .getOrElse(Response.notFound)
        .flatMap(_.attemptAs[Json].value.map(_.flatMap(_.as[DummyUser])))
        .unsafeRunSync() mustBe Right(dummyBob)
    }

    it should "fail on a timed out token" in {
      val response: OptionT[IO, Response[IO]] = for {
        auth     <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        timedOut <- OptionT.liftF(authSpec.timeoutAuthenticator(auth))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), timedOut)
        res <- testService(embedded)
      } yield res
      response
        .getOrElse(Response.notFound)
        .map(_.status)
        .unsafeRunSync() mustBe Status.Unauthorized
    }

    it should "work on a refreshed token" in {

      val response: OptionT[IO, Response[IO]] = for {
        auth    <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        expired <- OptionT.liftF(authSpec.timeoutAuthenticator(auth))
        renewed <- OptionT.liftF(authSpec.auth.refresh(expired))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), renewed)
        res <- testService(embedded)
      } yield res
      response
        .getOrElse(Response.notFound)
        .flatMap(_.attemptAs[Json].value.map(_.flatMap(_.as[DummyUser])))
        .unsafeRunSync() mustBe Right(dummyBob)
    }

    it should "Reject an invalid token" in {

      val response: OptionT[IO, Response[IO]] = for {
        auth <- OptionT.liftF(authSpec.wrongKeyAuthenticator)
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), auth)
        res <- testService(embedded)
      } yield res
      response
        .getOrElse(Response.notFound)
        .map(_.status)
        .unsafeRunSync() mustBe Status.Unauthorized
    }

    //note: we feed it "discarded" because stateless tokens rely on this.
    it should "Fail on a discarded token" in {
      val response: OptionT[IO, Response[IO]] = for {
        auth      <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        discarded <- OptionT.liftF(requestAuth.authenticator.discard(auth))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), discarded)
        res <- testService(embedded)
      } yield res
      response
        .getOrElse(Response.notFound)
        .map(_.status)
        .unsafeRunSync() mustBe Status.Unauthorized
    }

    it should "authorize for an allowed endpoint" in {
      val response: OptionT[IO, Response[IO]] = for {
        auth <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), auth)
        res <- everyoneService(embedded)
      } yield res
      response
        .getOrElse(Response[IO](status = Status.Forbidden))
        .flatMap(_.attemptAs[Json].value.map(_.flatMap(_.as[DummyUser])))
        .unsafeRunSync() mustBe Right(dummyBob)
    }

    it should "not authorize for a gated endpoint" in {
      val response: OptionT[IO, Response[IO]] = for {
        auth <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), auth)
        res <- adminService(embedded)
      } yield res
      response
        .getOrElse(Response.notFound)
        .map(_.status)
        .unsafeRunSync() mustBe Status.Unauthorized
    }

    it should "use the specified response when onNotAuthorized is specified" in {
      val req      = Request[IO](uri = Uri.unsafeFromString("/api"))
      val response = requestAuth.liftService(customService, _ => IO.pure(Response[IO](Status.BadGateway)))(req)

      response.getOrElse(Response.notFound).map(_.status).unsafeRunSync() mustBe Status.BadGateway
    }

    it should "catch unhandled errors into unauthorized" in {
      val response: OptionT[IO, Response[IO]] = for {
        auth <- OptionT.liftF(requestAuth.authenticator.create(dummyBob.id))
        embedded = authSpec.embedInRequest(Request[IO](uri = Uri.unsafeFromString("/api")), auth)
        res <- insaneService(embedded)
      } yield res
      response
        .getOrElse(Response[IO](status = Status.Forbidden))
        .map(_.status)
        .unsafeRunSync() mustBe Status.Unauthorized
    }

  }

}
