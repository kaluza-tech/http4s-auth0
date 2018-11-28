package com.ovoenergy.http4s.server.middleware.auth0

import cats.syntax.either._
import com.ovoenergy.http4s.server.middleware.auth0.Authenticator.Error._
import org.http4s._
import org.http4s.headers.Authorization
import pdi.jwt.{Jwt, JwtCirce, JwtOptions}

import scala.util.Try

/** Authenticator for Auth0 middleware
  *
  * @param config Configuration
  */
class Authenticator[F[_]](val config: Config) {

  import Authenticator._

  /** Authenticate an HTTP request
    *
    * @param request The HTTP request to authenticate
    * @return Either the answer to whether the request was authentic or possibly an error message
    */
  def authenticate(request: Request[F]): Result[AuthenticatedStatus] = {
    request.headers.get(Authorization) match {
      case None => AuthorizationHeaderNotFound().asLeft[AuthenticatedStatus]
      case Some(authorization) => validate(authorization)
    }
  }

  private def validate(authorization: Authorization): Result[AuthenticatedStatus] = {
    for {
      token <- extractToken(authorization)

      keyIdentity <- extractKeyIdentity(token)

      jwk <- Try(config.jwkProvider.get(keyIdentity.value))
        .toEither.left.map(exc => KeyIdentityNotFound(keyIdentity, exc))

      // $COVERAGE-OFF$ not sure how to test this right now we know jwk.getPublicKey can throw though
      publicKey <- Try(jwk.getPublicKey).toEither.left.map(InvalidPublicKey)
      // $COVERAGE-ON$

    } yield if(Jwt.isValid(token.value, publicKey)) Authenticated else NotAuthenticated
  }

  private def extractToken(authorization: Authorization): Result[BearerToken] = {
    // TODO: would be good to have a case class or BearerCredentials like the BasicCredentials in http4s
    val tokenRegex = """(?i)bearer\s+([^\s]+)""".r

    authorization.credentials.toString match {
      case tokenRegex(token) => BearerToken(token).asRight[Error]
      case _ => BearerTokenNotFound().asLeft[BearerToken]
    }
  }

  private def extractKeyIdentity(token: BearerToken): Result[KeyIdentity] = {
    val jwtOptions: JwtOptions = JwtOptions(signature = false)

    for {
      // NOTE: would be nice if this destructuring could be done in one line, Scala can't right now
      decoded <- JwtCirce.decodeJsonAll(token.value, jwtOptions).toEither.left.map(KeyIdentityDecodingFailure)

      header = decoded._1

      // TODO: could have a different error for this decoding failure, or change the message to be informative
      kid <- header.hcursor.get[String]("kid").left.map(KeyIdentityDecodingFailure)

    } yield KeyIdentity(kid)
  }
}

object Authenticator {
  type Result[A] = Either[Error, A]

  sealed trait AuthenticatedStatus
  case object Authenticated extends AuthenticatedStatus
  case object NotAuthenticated extends AuthenticatedStatus

  final case class KeyIdentity(value: String)

  final case class BearerToken(value: String)

  sealed trait Error extends Product with Serializable {
    def msg: String
  }

  object Error {

    final case class AuthorizationHeaderNotFound() extends Error {
      val msg: String = "no Authorization header present"
    }

    final case class BearerTokenNotFound() extends Error {
      val msg: String = "no Bearer token present"
    }

    final case class KeyIdentityNotFound(keyIdentity: KeyIdentity, cause: Throwable) extends Error {
      val msg: String = s"cannot find key identity '${keyIdentity.value}' - ${cause.getMessage}"
    }

    final case class InvalidPublicKey(cause: Throwable) extends Error {
      val msg: String = s"invalid public key - ${cause.getMessage}"
    }

    final case class KeyIdentityDecodingFailure(cause: Throwable) extends Error {
      val msg: String = s"key identity decoding failure - ${cause.getMessage}"
    }

  }

}
