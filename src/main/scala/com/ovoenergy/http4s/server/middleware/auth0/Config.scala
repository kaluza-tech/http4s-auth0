package com.ovoenergy.http4s.server.middleware.auth0

import com.auth0.jwk.JwkProvider
import org.http4s.Status

/**
  * @param jwkProvider JSON Web Key provider
  * @param unAuthorizedStatus HTTP status to return if this request cannot be authenticated with Auth0.
  *                           In some cases we would like this to be simply Unauthorized, in others we may
  *                           prefer NotFound to completely hide resources that the user does not have access to.
  */
final case class Config(jwkProvider: JwkProvider,
                        unAuthorizedStatus: Status = Status.Unauthorized)
