package tsec.authentication

import cats.effect.IO
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials}
import org.scalatest.prop.PropertyChecks
import tsec.cipher.symmetric.jca._
import tsec.jws.mac.{JWSMacCV, JWTMac}
import tsec.jwt.JWTClaims
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.mac.jca.{JCAMacTag, _}

class JWTAuthenticatorTests extends JWTAuthenticatorSpec with PropertyChecks {

  /** Stateful Bearer Auth **/
  AuthenticatorTest[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Stateful Bearer Authenticator",
    stateful[HMACSHA256]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Stateful Bearer Authenticator",
    stateful[HMACSHA384]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Stateful Bearer Authenticator",
    stateful[HMACSHA512]
  )
  requestAuthTests[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Stateful Bearer Authenticator",
    stateful[HMACSHA256]
  )
  requestAuthTests[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Stateful Bearer Authenticator",
    stateful[HMACSHA384]
  )
  requestAuthTests[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Stateful Bearer Authenticator",
    stateful[HMACSHA512]
  )

  /** End Stateful Bearer Auth **/
  /** Stateful Arbitrary Header Auth **/
  AuthenticatorTest[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Stateful Arbitrary Header Authenticator",
    statefulArbitraryH[HMACSHA256]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Stateful Arbitrary Header Authenticator",
    statefulArbitraryH[HMACSHA384]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Stateful Arbitrary Header Authenticator",
    statefulArbitraryH[HMACSHA512]
  )
  requestAuthTests[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Stateful Arbitrary Header Authenticator",
    statefulArbitraryH[HMACSHA256]
  )
  requestAuthTests[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Stateful Arbitrary Header Authenticator",
    statefulArbitraryH[HMACSHA384]
  )
  requestAuthTests[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Stateful Arbitrary Header Authenticator",
    statefulArbitraryH[HMACSHA512]
  )

  /** End Stateful Arbitrary Header Auth **/
  /** Basic Stateless tests **/
  AuthenticatorTest[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Stateless Authenticator",
    stateless[HMACSHA256]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Stateless Authenticator",
    stateless[HMACSHA384]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Stateless Authenticator",
    stateless[HMACSHA512]
  )

  requestAuthTests[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Stateless Authenticator",
    stateless[HMACSHA256]
  )
  requestAuthTests[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Stateless Authenticator",
    stateless[HMACSHA384]
  )
  requestAuthTests[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Stateless Authenticator",
    stateless[HMACSHA512]
  )

  /**End Basic Stateless tests **/
  /** Stateless Encrypted Arbitrary Header tests **/
  AuthenticatorTest[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Encrypted Stateless Authenticator",
    statelessEncrypted[HMACSHA256, AES128CTR]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Encrypted Stateless Authenticator",
    statelessEncrypted[HMACSHA384, AES128CTR]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Encrypted Stateless Authenticator",
    statelessEncrypted[HMACSHA512, AES128CTR]
  )

  requestAuthTests[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Encrypted Stateless Authenticator",
    statelessEncrypted[HMACSHA256, AES128CTR]
  )
  requestAuthTests[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Encrypted Stateless Authenticator",
    statelessEncrypted[HMACSHA384, AES128CTR]
  )
  requestAuthTests[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Encrypted Stateless Authenticator",
    statelessEncrypted[HMACSHA512, AES128CTR]
  )

  /** End Stateless Encrypted  Arbitrary Header Tests **/
  /** Stateless Encrypted Auth Bearer Header tests **/
  AuthenticatorTest[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Encrypted Bearer Token Stateless Authenticator",
    statelessBearerEncrypted[HMACSHA256, AES128CTR]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Encrypted Bearer Token Stateless Authenticator",
    statelessBearerEncrypted[HMACSHA384, AES128CTR]
  )
  AuthenticatorTest[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Encrypted Bearer Token Stateless Authenticator",
    statelessBearerEncrypted[HMACSHA512, AES128CTR]
  )

  requestAuthTests[AugmentedJWT[HMACSHA256, Int]](
    "HMACSHA256 JWT Encrypted Bearer Token Stateless Authenticator",
    statelessBearerEncrypted[HMACSHA256, AES128CTR]
  )
  requestAuthTests[AugmentedJWT[HMACSHA384, Int]](
    "HMACSHA384 JWT Encrypted Bearer Token Stateless Authenticator",
    statelessBearerEncrypted[HMACSHA384, AES128CTR]
  )
  requestAuthTests[AugmentedJWT[HMACSHA512, Int]](
    "HMACSHA512 JWT Encrypted Bearer Token Stateless Authenticator",
    statelessBearerEncrypted[HMACSHA512, AES128CTR]
  )

  /** End Stateless Encrypted Auth Bearer Header Tests **/
  def checkAuthHeader[A: JWTMacAlgo: JCAMacTag](implicit cv: JWSMacCV[IO, A], macKeyGen: MacKeyGen[IO, A]) = {
    behavior of JCAMacTag[A].algorithm + " JWT Token64 check"
    macKeyGen
      .generateKey
      .map { key =>
        it should "pass token68 check" in {
          forAll { (testSubject: String) =>
            val token = "Bearer "

            val (rawToken, parsed) = JWTMac
              .buildToString(JWTClaims(subject = Some(testSubject)), key)
              .map(s => (s, Authorization.parse(token + s)))
              .unsafeRunSync()

            parsed mustBe Right(Authorization(Credentials.Token(AuthScheme.Bearer, rawToken)))
          }

        }
      }
      .unsafeRunSync()
  }

  checkAuthHeader[HMACSHA256]
  checkAuthHeader[HMACSHA384]
  checkAuthHeader[HMACSHA512]

}
