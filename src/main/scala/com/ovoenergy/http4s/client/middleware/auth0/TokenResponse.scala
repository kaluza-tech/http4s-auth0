package com.ovoenergy.http4s.client.middleware.auth0

import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

final case class TokenResponse(accessToken: String)

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object TokenResponse {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val customDecoder: Decoder[TokenResponse] = deriveDecoder[TokenResponse]
  implicit val customEntityDecoder: EntityDecoder[IO, TokenResponse] = jsonOf[IO, TokenResponse]
}
