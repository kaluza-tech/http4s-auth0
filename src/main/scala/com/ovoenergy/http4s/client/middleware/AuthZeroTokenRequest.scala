package com.ovoenergy.http4s.client.middleware

import cats.effect.IO
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe._
import org.http4s.EntityEncoder
import org.http4s.circe._

final case class AuthZeroTokenRequest(audience: String,
                                      clientId: String,
                                      clientSecret: String,
                                      grantType: String = AuthZeroTokenRequest.DEFAULT_GRANT_TYPE)

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object AuthZeroTokenRequest {
  val DEFAULT_GRANT_TYPE = "client_credentials"
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val customEncoder: Encoder[AuthZeroTokenRequest] = deriveEncoder[AuthZeroTokenRequest]
  implicit val customEntityEncoder: EntityEncoder[IO, AuthZeroTokenRequest] = jsonEncoderOf
}
