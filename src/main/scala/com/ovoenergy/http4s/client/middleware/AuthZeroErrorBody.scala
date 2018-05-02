package com.ovoenergy.http4s.client.middleware

import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe._
import org.http4s.EntityEncoder
import org.http4s.circe._

final case class AuthZeroErrorBody(message: String)

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object AuthZeroErrorBody {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val customEncoder: Encoder[AuthZeroErrorBody] = deriveEncoder[AuthZeroErrorBody]

  implicit val customEntityEncoder: EntityEncoder[IO, AuthZeroErrorBody] = jsonEncoderOf[IO, AuthZeroErrorBody]
}

