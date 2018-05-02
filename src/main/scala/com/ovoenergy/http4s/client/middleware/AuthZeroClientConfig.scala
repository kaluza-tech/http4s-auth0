package com.ovoenergy.http4s.client.middleware

/** Auth0 Client Configuration
  * This is the config required to set up the [[AuthZeroClient]] middleware layer
  *
  * @param uri A base URI contianing just the `Domain` from the Auth0 Client settings page
  * @param audience The `Identifier` from the Auth0 Service settings page
  * @param id The `Client ID` from the Auth0 Client settings page
  * @param secret The `Client Secret` from the Auth0 Client settings page
  */
final case class AuthZeroClientConfig(uri: Uri, audience: String, id: String, secret: String)
