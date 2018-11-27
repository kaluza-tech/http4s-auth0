package com.ovoenergy.http4s.client.middleware.auth0

import java.net.ConnectException

import cats.data.{EitherT, Kleisli}
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import com.ovoenergy.http4s.client.middleware.auth0.Client.AuthZeroToken
import com.ovoenergy.http4s.client.middleware.auth0.Client.Error._
import org.http4s.circe._
import org.http4s.client.{DisposableResponse, Client => Http4sClient}
import com.ovoenergy.http4s.client.middleware.auth0.TokenResponse._
import org.http4s._

/**
  * HTTP4s Client middleware that transparently provides Auth0 authentication
  *
  * @todo The request for a new token would be better if it could only ever be
  *       performing a single request so that multiple threads would not kick
  *       off multiple requests to the Auth0 backend to generate a new one if
  *       the token was not present or had become invalid.
  * @todo Clean up case-logic for retry into something neater
  */
class Client[F[_]: Sync] private (val config: Config, val client: Http4sClient[F], currentToken: Ref[F, Option[AuthZeroToken]]) {
  private implicit val authZeroErrorBodyEntityEncoder: EntityEncoder[F, ErrorBody] = jsonEncoderOf

  import Client._

  def open(req: Request[F]): F[DisposableResponse[F]] =
  for {
    authToken <- currentToken.get
    retry <- retryRequest(req, authToken, 1)
  } yield retry match {
    case Right((response, token)) =>
      currentToken.modify(v => (v, Some(token)))
      response
    case Left(err) =>
      currentToken.modify(v => (v, None))
      errorResponse(err)
    }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def retryRequest(req: Request[F], maybeToken: Option[AuthZeroToken], retries: Int): F[Result[ResponseAndToken[F]]] = {
    val result: F[Result[ResponseAndToken[F]]] = (for {
      token <- EitherT(eitherToken(maybeToken))
      result <- EitherT(performRequest(req, token))
    } yield result).value

    result.flatMap({
      case Left(_) if retries > 0 => retryRequest(req, None, retries - 1)
      case Left(err) => Sync[F].pure(err.asLeft[ResponseAndToken[F]])
      case result@Right(_) => Sync[F].pure(result)
    })
  }

  private def performRequest(req: Request[F], token: AuthZeroToken): F[Result[ResponseAndToken[F]]] = {
    client.open(enhanceRequest(req, token)).flatMap(disposableResponse => {
      disposableResponse.response.status match {
        case Status.Unauthorized => requestNotAuthorized(disposableResponse)
        case Status.NotFound => requestNotAuthorized(disposableResponse)
        case _ => Sync[F].pure((disposableResponse, token).asRight[Error])
      }
    })
  }

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  private def requestNotAuthorized(disposableResponse: DisposableResponse[F]): F[Result[ResponseAndToken[F]]] =
    disposableResponse.dispose.attempt.map{ _ => NotAuthorized().asLeft[ResponseAndToken[F]]} // swallow any exception and return NotAuthorised

  private def eitherToken(maybeToken: Option[AuthZeroToken]): F[Result[AuthZeroToken]] =
    maybeToken.map(token => Sync[F].pure(token.asRight[Error])).getOrElse(generateToken())

  private def generateToken(): F[Result[AuthZeroToken]] = {
    val request = TokenRequest(config.audience, config.id, config.secret)
    implicit val tokenRequestEncoder: EntityEncoder[F, TokenRequest] = jsonEncoderOf
    implicit val customEntityDecoder: EntityDecoder[F, TokenResponse] = jsonOf[F, TokenResponse]

    val uri: Uri = config.uri / "oauth" / "token"

    client
      .expect[TokenResponse](Request[F](method = Method.POST, uri = uri).withEntity(request))
      .map(_.accessToken.asRight[Error])
      .handleError {
        case e: ConnectException => AuthZeroUnavailable(e).asLeft[AuthZeroToken]
        // TODO: could add more granularity to error handling here
        case _ => NotAuthorized().asLeft[AuthZeroToken]
      }
  }

  private def enhanceRequest(req: Request[F], token: AuthZeroToken): Request[F] = req.putHeaders(Header("Authorization", s"Bearer $token"))

  private def errorResponse(err: Error): DisposableResponse[F] = {
    val status = err match {
      case NotAuthorized() => Status.Unauthorized
      case AuthZeroUnavailable(_) => Status.RequestTimeout
    }

    val entityResponse = Response[F](status = status).withEntity(ErrorBody(err.msg))

    DisposableResponse(entityResponse, nullOpDispose)
  }

  private val nullOpDispose = Sync[F].pure(())
}

object Client {

  type ResponseAndToken[F[_]] = (DisposableResponse[F], AuthZeroToken)

  type AuthZeroToken = String

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  def apply[F[_]: Sync](config: Config)(client: Http4sClient[F]): Http4sClient[F] = {
    val authClient = Ref.of[F, Option[AuthZeroToken]](None).map(t => new Client(config, client, t))

    def authenticatedOpen(req: Request[F]): F[DisposableResponse[F]] = {
      authClient.flatMap(_.open(req))
    }

    client.copy(open = Kleisli(authenticatedOpen))
  }

  sealed trait Error extends Product with Serializable {
    def msg: String
  }

  type Result[T] = Either[Error, T]

  object Error {

    final case class NotAuthorized() extends Error {
      val msg: String = "The credentials you presented have not been accepted by Auth0"
    }

    final case class AuthZeroUnavailable(cause: ConnectException) extends Error {
      val msg: String = s"Auth0 cannot be contacted to validate your credentials - ${cause.getMessage}"
    }

  }

  def create[F[_]: Sync](config: Config, http4sClient: Http4sClient[F]): F[Client[F]] =
    for {
      currentToken <- Ref.of[F, Option[AuthZeroToken]](None)
      client = new Client[F](config, http4sClient, currentToken)
    } yield client

}
