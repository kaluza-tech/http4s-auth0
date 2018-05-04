package com.ovoenergy.http4s.client.middleware.auth0

import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe._
import org.http4s.EntityEncoder
import org.http4s.circe._

final case class ErrorBody(message: String)

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object ErrorBody {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val customEncoder: Encoder[ErrorBody] = deriveEncoder[ErrorBody]

  implicit val customEntityEncoder: EntityEncoder[IO, ErrorBody] = jsonEncoderOf[IO, ErrorBody]
}

