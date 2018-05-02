package com.ovoenergy.http4s.client.middleware

final case class AuthZeroErrorBody(message: String)

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object AuthZeroErrorBody {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val customEncoder: Encoder[AuthZeroErrorBody] = deriveEncoder[AuthZeroErrorBody]

  implicit val customEntityEncoder: EntityEncoder[IO, AuthZeroErrorBody] = jsonEncoderOf[IO, AuthZeroErrorBody]
}

