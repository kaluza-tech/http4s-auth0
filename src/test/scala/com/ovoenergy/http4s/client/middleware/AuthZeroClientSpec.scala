package com.ovoenergy.evse.service.client.middleware

import cats.effect.IO
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.http4s.client.Client
import org.http4s.{Method, Status, Uri}
import org.http4s.client.blaze.Http1Client
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest._
import org.http4s._
import org.http4s.client._
import com.github.tomakehurst.wiremock.stubbing.Scenario._
import fs2.Scheduler

class AuthZeroClientSpec extends
  fixture.WordSpec with Matchers with EitherValues with GeneratorDrivenPropertyChecks with BeforeAndAfterAll {

  implicit val (scheduler, shutDown): (Scheduler, IO[Unit]) = Scheduler.allocate[IO](corePoolSize = 2, threadPrefix = "service-client-workers").unsafeRunSync()

  "AuthZeroClient" when {

    "configured with good credentials" should {

      "succeed when auth0 is available" in { server =>
        val config = defaultConfig()

        stubFor(post(urlEqualTo(authZeroUri.path))
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(aResponse().withBody(authZeroResponseBody(token))))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        val client = createSubject()

        val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

        val result = client.expect[String](request).attempt.unsafeRunSync()

        result.right.value shouldEqual resourceBody
      }

      "request only one token from Auth0 for multiple requests" in { server =>
        val config = defaultConfig()

        stubFor(post(urlEqualTo(authZeroUri.path))
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(aResponse().withBody(authZeroResponseBody(token))))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        val client = createSubject()

        val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

        val firstResult = client.expect[String](request).attempt.unsafeRunSync()

        val secondResult = client.expect[String](request).attempt.unsafeRunSync()

        firstResult.right.value shouldEqual resourceBody

        secondResult.right.value shouldEqual resourceBody

        verify(lessThanOrExactly(1), postRequestedFor(urlEqualTo(authZeroUri.path)))
      }


      "retry for a new token if the one it has results in a Not Found response" in { server =>
        val config = defaultConfig()

        val firstToken = "first-token"

        stubFor(post(urlEqualTo(authZeroUri.path))
          .inScenario("Simulate Not Found")
          .whenScenarioStateIs(STARTED)
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(aResponse().withBody(authZeroResponseBody(firstToken)))
          .willSetStateTo("First Token Generated"))

        stubFor(post(urlEqualTo(authZeroUri.path))
          .inScenario("Simulate Not Found")
          .whenScenarioStateIs("First Token Generated")
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(aResponse().withBody(authZeroResponseBody(token))))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(firstToken)))
          .willReturn(notFound()))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        val client = createSubject()

        val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

        val result = client.expect[String](request).attempt.unsafeRunSync()

        result.right.value shouldEqual resourceBody

        verify(moreThan(1), postRequestedFor(urlEqualTo(authZeroUri.path)))
      }

      "retry for a new token if the one it has results in an Unauthorized response" in { server =>
        val config = defaultConfig()

        val firstToken = "first-token"

        stubFor(post(urlEqualTo(authZeroUri.path))
          .inScenario("Simulate Not Found")
          .whenScenarioStateIs(STARTED)
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(aResponse().withBody(authZeroResponseBody(firstToken)))
          .willSetStateTo("First Token Generated"))

        stubFor(post(urlEqualTo(authZeroUri.path))
          .inScenario("Simulate Not Found")
          .whenScenarioStateIs("First Token Generated")
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(aResponse().withBody(authZeroResponseBody(token))))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(firstToken)))
          .willReturn(unauthorized()))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        val client = createSubject()

        val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

        val result = client.expect[String](request).attempt.unsafeRunSync()

        result.right.value shouldEqual resourceBody

        verify(moreThan(1), postRequestedFor(urlEqualTo(authZeroUri.path)))
      }

      "fail when auth0 is unavailable" in { server =>
        val config = AuthZeroClientConfig(invalidUri, "audience", "client-identity", "client-secret")

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        val client = createSubject(config = config)

        val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

        val result = client.expect[String](request).attempt.unsafeRunSync()

        result.left.value shouldEqual UnexpectedStatus(Status.RequestTimeout)
      }
    }

    "configured without good credentials" should {

      "fail when auth0 responds that they are not authorized" in { server =>
        val config = defaultConfig()

        stubFor(post(urlEqualTo(authZeroUri.path))
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(unauthorized()))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        val client = createSubject()

        val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

        val result = client.expect[String](request).attempt.unsafeRunSync()

        result.left.value shouldEqual UnexpectedStatus(Status.Unauthorized)
      }
    }
  }

  private val httpClient: Client[IO] = Http1Client[IO]().unsafeRunSync()

  private val authorizationHeader = "Authorization"

  private val host = "localhost"

  private def baseUri: Uri = Uri.unsafeFromString(s"http://$host:${wireMockServer.port()}/")

  private def resourceUri: Uri = baseUri / "resource" / "sub-resource"

  private def authZeroUri: Uri = baseUri / "oauth" / "token"

  private def invalidUri: Uri = Uri.unsafeFromString(s"http://$host/")

  private val token = "GOOD-TOKEN"

  private val resourceBody = "Hello World"

  private def defaultConfig() = AuthZeroClientConfig(baseUri, "audience", "client-identity", "client-secret")

  private def createSubject(config: AuthZeroClientConfig = defaultConfig()): Client[IO] = {
    AuthZeroClient(config)(httpClient)
  }

  private def bearerToken(token: String): String = s"Bearer $token"

  private def authZeroRequestBody(config: AuthZeroClientConfig): String =
      s"""
        |{
        |  "audience": "${config.audience}",
        |  "client_id": "${config.id}",
        |  "client_secret": "${config.secret}",
        |  "grant_type": "client_credentials"
        |}
      """.stripMargin

  private def authZeroResponseBody(token: String): String =
    s"""
       |{
       |  "access_token": "$token",
       |  "grant_type": "client_credentials",
       |  "scope": "read:all",
       |  "expires_in": 86400,
       |  "token_type": "Bearer"
       |}
      """.stripMargin

  private val wireMockOptions = options.dynamicPort().bindAddress(host)

  private val wireMockServer = new WireMockServer(wireMockOptions)

  type FixtureParam = WireMockServer

  override def beforeAll(): Unit = wireMockServer.start()

  override def afterAll(): Unit = {
    wireMockServer.stop()
    shutDown.unsafeRunSync()
  }

  def withFixture(test: OneArgTest): Outcome = {
    val server = wireMockServer
    try {
      configureFor(host, server.port())
      test(server)
    }
    finally {
      server.resetAll()
    }
  }
}
