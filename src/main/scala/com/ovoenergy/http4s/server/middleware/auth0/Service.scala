package com.ovoenergy.http4s.server.middleware.auth0

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import org.http4s._

/** http4s service middleware to transparently authenticate using Auth0 JWT tokens
  */
object Service {
  import Authenticator._

  @SuppressWarnings(Array("org.wartremover.warts.Nothing","org.wartremover.warts.Any"))
  def apply(service: HttpService[IO], authenticator: Authenticator): HttpService[IO] = {
    Kleisli { req =>
      authenticator.authenticate(req) match {
        case Right(Authenticated) =>
          service.run(req)
        case Right(NotAuthenticated) =>
          // TODO: make the response code returned when not authenticated configurable
          OptionT.pure(Response[IO](status = Status.NotFound))
        case Left(_) =>
          // TODO: logging would make debugging auth errors much easier
          // TODO: make this configurable? - unauthorized vs notfound
          OptionT.pure(Response[IO](status = Status.NotFound))
      }
    }
  }
}
