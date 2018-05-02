package com.ovoenergy.http4s.client.middleware

final case class AuthZeroTokenResponse(accessToken: String)

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object AuthZeroTokenResponse {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val customDecoder: Decoder[AuthZeroTokenResponse] = deriveDecoder[AuthZeroTokenResponse]
  implicit val customEntityDecoder: EntityDecoder[IO, AuthZeroTokenResponse] = jsonOf[IO, AuthZeroTokenResponse]
}
