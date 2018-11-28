package com.ovoenergy.http4s.server.middleware.auth0

import cats.Applicative
import cats.data.{Kleisli, OptionT}
import org.http4s._

/**
  * http4s service middleware to transparently authenticate using Auth0 JWT tokens
  */
object Service {
  import Authenticator._

  @SuppressWarnings(Array("org.wartremover.warts.Nothing","org.wartremover.warts.Any"))
  def apply[F[_]: Applicative](service: HttpRoutes[F], config: Config): HttpRoutes[F] = {
    val authenticator: Authenticator[F] = new Authenticator(config)

    Kleisli { req =>
      authenticator.authenticate(req) match {
        case Right(Authenticated) =>
          service.run(req)
        case Right(NotAuthenticated) =>
          OptionT.pure[F](Response[F](status = config.unAuthorizedStatus))
        case Left(_) =>
          // TODO: logging would make debugging auth errors much easier
          OptionT.pure[F](Response[F](status = config.unAuthorizedStatus))
      }
    }
  }
}
