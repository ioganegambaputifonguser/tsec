import java.time.Instant

object JWTMacExamples {

  import cats.effect.Sync
  import cats.syntax.all._
  import tsec.jws.mac._
  import tsec.jwt._
  import tsec.mac.jca._

  import scala.concurrent.duration._

  /** You can interpret into any target Monad with an instance of Sync[F] using JwtMac */
  def jwtMonadic[F[_]: Sync]: F[JWTMac[HMACSHA256]] =
    for {
      key             <- HMACSHA256.generateKey[F]
      claims          <- JWTClaims.withDuration[F](expiration = Some(10.minutes))
      jwt             <- JWTMac.build[F, HMACSHA256](claims, key) //You can sign and build a jwt object directly
      verifiedFromObj <- JWTMac.verifyFromInstance[F, HMACSHA256](jwt, key) //Verify from an object directly
      stringjwt       <- JWTMac.buildToString[F, HMACSHA256](claims, key) //Or build it straight to string
      isverified      <- JWTMac.verifyFromString[F, HMACSHA256](stringjwt, key) //You can verify straight from a string
      parsed          <- JWTMac.verifyAndParse[F, HMACSHA256](stringjwt, key) //Or verify and return the actual instance
    } yield parsed

  import io.circe._
  import io.circe.generic.semiauto._
  import io.circe.syntax._

  case class Doge(suchChars: String, much32Bits: Int, so64Bits: Long)

  object Doge {
    implicit val encoder: ObjectEncoder[Doge] = deriveEncoder[Doge]
    implicit val decoder: Decoder[Doge]       = deriveDecoder[Doge]
    val WowSuchClaim                          = "Doge"
  }

  JWTClaims(customFields = Seq(Doge.WowSuchClaim -> Doge("w00f", 8008135, 80085L).asJson))

  def builderStuff[F[_]: Sync]: F[JWTClaims] =
    Sync[F].pure(JWTClaims(customFields = List(Doge.WowSuchClaim -> Doge("w00f", 8008135, 80085L).asJson)))

  /** encoding custom claims **/
  def jwtWithCustom[F[_]: Sync]: F[(JWTMac[HMACSHA256], Doge)] =
    for {
      key <- HMACSHA256.generateKey[F]
      claims <- JWTClaims
        .withDuration[F](
          expiration = Some(10.minutes),
          customFields = List(Doge.WowSuchClaim -> Doge("w00f", 8008135, 80085L).asJson)
        )
      jwt             <- JWTMac.build[F, HMACSHA256](claims, key)
      verifiedFromObj <- JWTMac.verifyFromInstance[F, HMACSHA256](jwt, key)
      stringjwt       <- JWTMac.buildToString[F, HMACSHA256](claims, key) //Or build it straight to string
      isverified      <- JWTMac.verifyFromString[F, HMACSHA256](stringjwt, key) //You can verify straight from a string
      parsed          <- JWTMac.verifyAndParse[F, HMACSHA256](stringjwt, key)
      doge            <- parsed.body.getCustomF[F, Doge](Doge.WowSuchClaim)
    } yield (parsed, doge)

  /** Using impure either interpreters */
  val impureClaims = JWTClaims(expiration = Some(Instant.now.plusSeconds(10.minutes.toSeconds)))

  val jwt: Either[Throwable, JWTMac[HMACSHA256]] = for {
    key             <- HMACSHA256.generateKey[MacErrorM]
    jwt             <- JWTMacImpure.build[HMACSHA256](impureClaims, key) //You can sign and build a jwt object directly
    verifiedFromObj <- JWTMacImpure.verifyFromInstance[HMACSHA256](jwt, key)
    stringjwt       <- JWTMacImpure.buildToString[HMACSHA256](impureClaims, key) //Or build it straight to string
    isverified      <- JWTMacImpure.verifyFromString[HMACSHA256](stringjwt, key) //You can verify straight from a string
    parsed          <- JWTMacImpure.verifyAndParse[HMACSHA256](stringjwt, key) //Or verify and return the actual instance
  } yield parsed

}
