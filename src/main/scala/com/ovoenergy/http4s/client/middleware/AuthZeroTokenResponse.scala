package com.ovoenergy.http4s.client.middleware

import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

final case class AuthZeroTokenResponse(accessToken: String)

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object AuthZeroTokenResponse {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val customDecoder: Decoder[AuthZeroTokenResponse] = deriveDecoder[AuthZeroTokenResponse]
  implicit val customEntityDecoder: EntityDecoder[IO, AuthZeroTokenResponse] = jsonOf[IO, AuthZeroTokenResponse]
}
