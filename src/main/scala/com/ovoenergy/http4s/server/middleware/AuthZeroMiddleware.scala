package com.ovoenergy.http4s.server.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.either._
import com.auth0.jwk.JwkProvider
import com.ovoenergy.http4s.server.middleware.AuthZeroAuthenticator.Error._
import org.http4s._
import org.http4s.headers.Authorization
import pdi.jwt.{Jwt, JwtCirce, JwtOptions}

import scala.util.Try

/** Authenticator for AuthZero middleware
  *
  * @param jwkProvider JSON Web Key provider
  */
class AuthZeroAuthenticator(val jwkProvider: JwkProvider) {

  import AuthZeroAuthenticator._

  /** Authenticate an HTTP request
    *
    * @param request The HTTP request to authenticate
    * @return Either the answer to whether the request was authentic or possibly an error message
    */
  def authenticate(request: Request[IO]): Result[AuthenticatedStatus] = {
    request.headers.get(Authorization) match {
      case None => AuthorizationHeaderNotFound().asLeft[AuthenticatedStatus]
      case Some(authorization) => validate(authorization)
    }
  }

  private def validate(authorization: Authorization): Result[AuthenticatedStatus] = {
    for {
      token <- extractToken(authorization)

      keyIdentity <- extractKeyIdentity(token)

      jwk <- Try(jwkProvider.get(keyIdentity.value))
        .toEither.left.map(exc => KeyIdentityNotFound(keyIdentity, exc))

      // TODO: this is an external dependency, when we split this out of this codebase it should be easier to better
      // tests around the external services requried for Auth0
      // $COVERAGE-OFF$
      publicKey <- Try(jwk.getPublicKey).toEither.left.map(InvalidPublicKey)
      // $COVERAGE-ON$
    } yield Jwt.isValid(token.value, publicKey) match {
      case true => Authenticated
      case false => NotAuthenticated
    }
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
      decoded <- JwtCirce.decodeJsonAll(token.value, jwtOptions).toEither.left.map(DecodingFailure)
      (header, _, _) = decoded

      kid <- header.hcursor.get[String]("kid").left.map(DecodingFailure)

    } yield KeyIdentity(kid)
  }
}

object AuthZeroAuthenticator {
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
      val msg: String = s"cannot find key identity ${keyIdentity.value} - exception: ${cause.getMessage}"
    }

    // $COVERAGE-OFF$
    final case class InvalidPublicKey(cause: Throwable) extends Error {
      val msg: String = cause.getMessage
    }
    // $COVERAGE-ON$

    final case class DecodingFailure(cause: Throwable) extends Error {
      val msg: String = cause.getMessage
    }

  }

}

/** http4s middleware to transparently authenticate using Auth0 JWT tokens
  *
  * TODO: generalise and extract from this project
  */
object AuthZeroMiddleware {
  import com.ovoenergy.http4s.server.middleware.AuthZeroAuthenticator._
  def apply(service: HttpService[IO], authenticator: AuthZeroAuthenticator): HttpService[IO] = {
    Kleisli { req =>
      authenticator.authenticate(req) match {
        case Right(Authenticated) =>
          service.run(req)
        case Right(NotAuthenticated) =>
          OptionT.pure(Response[IO](status = Status.NotFound))
        case Left(_) =>
          // TODO: logging would make debugging auth errors much easier
          // TODO: make this configurable? - unauthorized vs notfound
          OptionT.pure(Response[IO](status = Status.NotFound))
      }
    }
  }
}
