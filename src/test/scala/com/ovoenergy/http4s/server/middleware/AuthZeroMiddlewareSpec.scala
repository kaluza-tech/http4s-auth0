package com.ovoenergy.evse
package middleware

import com.auth0.jwk.UrlJwkProvider
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.ovoenergy.evse.scalatest.matchers.Http4sMatchers
import com.ovoenergy.http4s.server.middleware.{AuthZeroAuthenticator, AuthZeroMiddleware}
import org.http4s.Credentials.Token
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.util.CaseInsensitiveString
import org.scalatest.{BeforeAndAfterAll, OptionValues, WordSpec}

class AuthZeroMiddlewareSpec extends WordSpec with Http4sMatchers with OptionValues with BeforeAndAfterAll {

  private val simpleService = HttpService[IO] {
    case _ -> Root / "resource" => Ok()
  }

  private val baseRequest = Request[IO](GET, uri("/resource"))

  "AuthZeroMiddleware" should {

    "return 404 if no Authorization header is provided" in {
      val service = AuthZeroMiddleware(simpleService, createAuthenticator)
      val request = baseRequest.withHeaders(Headers.empty)
      service.run(request).value.unsafeRunSync() should haveStatus(Status.NotFound)
    }

    "return 404 if no Bearer token is present in Authorization header" in {
      val service = AuthZeroMiddleware(simpleService, createAuthenticator)
      val request = baseRequest.withHeaders(Headers(basicTokenHeader("basic-credentials")))
      service.run(request).value.unsafeRunSync() should haveStatus(Status.NotFound)
    }

    "return 404 if a bad Bearer token is supplied" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjJ0TEJuLVBtSHgzdXRfRkQ5dmJkSmtLNVUzV1FEMzk2US1nMExpRlg5U0kifQ.eyJpc3MiOiJldnNlIGRldmljZSBpbnRlZ3JhdGlvbiIsIm5iZiI6MTUxMzU0NDkwM30.OUK-Ipty21TIILhLg-ImFhNpfabXCb0BB-_eASjuj-QcVZdOZb42CIZAkSpjGbpScFjcBcM_kZh3OJnxP3uhj9hCRDqfDiu2YpoXitLwcNlvf_I-XfZxGCVFxZSiktcu3TMqL1JIPNWwpZoSIZvo3d0ubXmlxdjfXdGVC7Rk_YWjKWjVpxLOWuPfKplsCtpe07DTcpIkazzr7Nx4eFcrxajZUlBqgxPSznvTJN0jcaegB8_-HhoYyQPosox3haKFKxf1Cg9MslmVq746lTZWlDlq5no3WyIXmLiizWsit1fuSHFZIeWnIYQqsksgKNe0z_6Kv4XEsHlIHeXKgw0FJA"
      val service = AuthZeroMiddleware(simpleService, createAuthenticator)
      val request = baseRequest.withHeaders(Headers(bearerTokenHeader(token)))
      service.run(request).value.unsafeRunSync() should haveStatus(Status.NotFound)
    }

    "return 404 if an unsigned Bearer token is supplied" in {
      val token= "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjZfWWZvbHh4elJZUDR5cmdFSWlOaXRVNzNpUEVGNnlhQTdVR29NNk5KRXcifQ.eyJpc3MiOiJldnNlIGRldmljZSBpbnRlZ3JhdGlvbiIsIm5iZiI6MTUxMzU5MDU3NX0."
      val service = AuthZeroMiddleware(simpleService, createAuthenticator)
      val request = baseRequest.withHeaders(Headers(bearerTokenHeader(token)))
      service.run(request).value.unsafeRunSync() should haveStatus(Status.NotFound)
    }

    "return 404 if no 'kid' present in supplied Bearer token" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJldnNlIGRldmljZSBpbnRlZ3JhdGlvbiIsIm5iZiI6MTUxMzU5MDgxMH0.X4aacxq-E-SMyUGjXfbfPG0UZdYXXj6t_1Y47-8C6SPaQcsKE5g8YYMoaASu5KQU1php_BMKEwkiC1zVukEviCOcWw3ihhM9l9gogK99EdJF0BYTk8kkRXoM42QIED3ORScod-JcHGv2_yYVucaUMC1Fw-YAekyhxyMwBahrBsNAyDRCvZeE4yO9QrlFEb_KZkAaxYCuL_2o9ObWJ3EhpJSWFgeZUzD1uZ7SEPR9677HHPmaO8XCy_W76XjE5O17uacquFlOYAORkQAqqIYnVIlDyO5qw58Mk1vfAOskycOd0hLLzTjwItsZmQhqSOCrgOjz69XTWw2qrVvm0iGRQA"
      val service = AuthZeroMiddleware(simpleService, createAuthenticator)
      val request = baseRequest.withHeaders(Headers(bearerTokenHeader(token)))
      service.run(request).value.unsafeRunSync() should haveStatus(Status.NotFound)
    }

    "return 200 if a good Bearer token is supplied" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjZfWWZvbHh4elJZUDR5cmdFSWlOaXRVNzNpUEVGNnlhQTdVR29NNk5KRXcifQ.eyJpc3MiOiJldnNlIGRldmljZSBpbnRlZ3JhdGlvbiIsIm5iZiI6MTUxMzQ0MDIxOX0.O0CO-fiNWiqCYGkrU0AuJiLVcCGPEEFKWFHvYg1yrmo2bIu0dIlAluL2ZpHk1a5N1dj-mG1qNnhxaNsTMXjJVxFLRquS58OPI1_cJUwdBbGijUm8eH1Dh1VTeKqg5unqeeZ6baoqqWrKv98B_QxpafQhVAthyj18c_KfrXIKGdVQCpKyY6_2B_3b7-f0iUn17S2-VTRwARzRphHoqY27HR7n4mmkSFvuhMEnE5coOuv4z4Hxx8ENYksWSQ0k4qb42qE03YNX7h7Sf0IsaclJgb6VZHkiX7p1RZIb_xjOVIABF-HCj0J7E77vNH9wqJAhbRITNV1Qp737D7fexYYjXA"
      val service = AuthZeroMiddleware(simpleService, createAuthenticator)
      val request = baseRequest.withHeaders(Headers(bearerTokenHeader(token)))
      service.run(request).value.unsafeRunSync() should haveStatus(Status.Ok)
     }
  }

  private val port = 8080

  private val wireMockServer = new WireMockServer(options().port(port))

  private val jwksDomain: String = s"http://localhost:$port/"

  private val jwksPath: String = "/.well-known/jwks.json"

  private def bearerTokenHeader(token: String): Header = Authorization(Token(CaseInsensitiveString("bearer"), token))

  private def basicTokenHeader(token: String): Header = Authorization(Token(CaseInsensitiveString("basic"), token))

  private def createAuthenticator: AuthZeroAuthenticator = new AuthZeroAuthenticator(new UrlJwkProvider(jwksDomain))

  override def beforeAll(): Unit = {
    wireMockServer.start()
    stubFor(get(urlEqualTo(jwksPath))
      .willReturn(aResponse()
        .withBody(jwksString)
        .withHeader("Content-Type", "application/json")))
    ()
  }

  override def afterAll(): Unit = {
    wireMockServer.resetAll()
    wireMockServer.stop()
  }

  private val jwksString: String =
    s"""
       |{
       |  "keys":[
       |    {
       |      "kty":"RSA",
       |      "e":"AQAB",
       |      "n":"qqEIlRff7tvVVLd9bYqwjN7MIvZ6Vr6gk5Iz1XIV2OQKOnsyYQqOLaLAz0SuWQhuxS6yELBZGkeTY19DCL20bqL0wAWSDbY3nIOKA22Jz6PT0_QUmsZA_6CS38Q1AJgVnxKHmA072MOmlPFrXs_UQYEp49rl0ZOH5yzw3331l6miL89Vak4b7alBqrz4AzpwKbhURCOiVggrjI0eyLyVCMP3IriARqcyDZnfabrb_1zqzLh7LeX9xZgCzMmZLzXDv9lckUoYmr0rB8fkljrSaeUyC2t9N68_hYKbZvZD1H3R2bQbwYtuIhuTDs5m_rNQ5hC10eeTfWKsCHmWD2n-9w",
       |      "d":"Ay8kINwkMqnpjj4qWgv3hXl258QfvlSqXHai_v21CKogwryf0Q3IX3OlE3kdo82ze3yGmCciUoo5ZuZVMumPX20tEhRIiHb5DLOrtKqrorNU9aSdadH_ToXpl8Fql8s-xAwteZntnvrvteHXdhp8xIgrhvQDy-hr5dWB3TnVhnH9JjsWEeJtITUUT3Chvk6Qr5iaoHxeE8GBL1mONq_311UCsA_0dDnjrTxHk0Rl_UEW8IgvgUnX47IC_XDFo2mxaUgnfp6HYvF8bgu8ReZft-Uana39PCvG5-a2EqejRANEE6rfGd37_q74CYWKyotOHqIOjmGbU550P2qUAFy74Q",
       |      "p":"1n3x0dthS1n_4u0pkdzd10ONQuLADIsAwb0j770FRU4W1sgQ6njIyYAoqbypUqcSmgCIh6z6u8wI8qSdlLiaf0jppo4D_rNJ3GgPD8JulCa6-MuvBSdNrnbBnTsgbnhzzNW_Dv6aXBPQsFN4TKzEzTCvX7pkIuxXBGZ761awQTE",
       |      "q":"y6YYD5m0trKY70XbKGmy1tMPLnUQ_qMi2Mft_CE89Qe80kMTfKXRCD0SbT23DlYuOdn58EO5Zul2OECAxqzAWqhtWb_eETkcx4AUcoXLfF_hEERshq7VuVCPmTOkCBakeuCXvbFXP4xPwi8t2P5kbsoQ6tPeWKgd5MgfsuXI-Kc",
       |      "dp":"1Up0d6zh-V5L2MrhtYyehXtFM6fsNgzb-cwtui6K2Ton9_kG6UKm18k7pl5XOjutgbraCaD3zFu-4hrUIJnZ-Iz224sJafO7lRNdNtyvvV8uOk3BgyxsUPsC2Wl8fJ4G-T_sN-rycHG76jt2uzhqk2jAXD8raJcAiP2oaIPRzWE",
       |      "dq":"PH6ebyPttsfe_lT2AU9EvOP9goNsBPhZBaR-YYNOJEukH6GlTEXFjnj13FWU50FagfzqbiDwFk4LvDINDveKPGQD98EzKlKY5fp6GHr-M3gX10k8I9YTzvTLc2sPswdm3MmSydrAXDRGDR0OtYL8Opzz4Y4GYreuCaQZJn9z78k",
       |      "qi":"S7AG_u4jDVQ7a_vmy_Sqw0Mk1r1DZotUedWay-YYAZszOSs-8RrvxxYsAnON5c0DmRIAIzTgrdAvAnqRO4Yr-3jmZ8b8T0E4xVcNsw9Tg4jvi_OH5w1wt-xnZ-AW7cXAkX7Jyri8cFCr9Lzt9XnwvTjGh7aS6q1ePQQv0fhEA10",
       |      "kid":"6_YfolxxzRYP4yrgEIiNitU73iPEF6yaA7UGoM6NJEw"
       |    }
       |  ]
       |}
     """.stripMargin
}
