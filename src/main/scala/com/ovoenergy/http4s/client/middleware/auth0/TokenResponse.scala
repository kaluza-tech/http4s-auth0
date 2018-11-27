package com.ovoenergy.http4s.client.middleware.auth0

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe._

final case class TokenResponse(accessToken: String)

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object TokenResponse {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val customDecoder: Decoder[TokenResponse] = deriveDecoder[TokenResponse]
}
