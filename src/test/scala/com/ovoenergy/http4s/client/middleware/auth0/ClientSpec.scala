package com.ovoenergy.http4s.client.middleware.auth0

import scala.concurrent.ExecutionContext.global

import cats.effect._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.stubbing.Scenario._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.{Client => Http4sClient, _}
import org.http4s.{Method, Status, Uri, _}
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class ClientSpec extends
  WordSpec with Matchers with EitherValues with GeneratorDrivenPropertyChecks with BeforeAndAfterAll with BeforeAndAfterEach {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  "Client" when {

    "configured with good credentials" should {

      "succeed when auth0 is available" in {
        val config = defaultConfig()

        stubFor(post(urlEqualTo(authZeroUri.path))
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(aResponse().withBody(authZeroResponseBody(token))))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        testWithClient(config) { client =>
          val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

          val result = client.expect[String](request).attempt.unsafeRunSync()

          result.right.value shouldEqual resourceBody
        }
      }

      "request only one token from Auth0 for multiple requests" in {
        val config = defaultConfig()

        stubFor(post(urlEqualTo(authZeroUri.path))
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(aResponse().withBody(authZeroResponseBody(token))))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        testWithClient(config) { client =>
          val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

          val firstResult = client.expect[String](request).attempt.unsafeRunSync()

          val secondResult = client.expect[String](request).attempt.unsafeRunSync()

          firstResult.right.value shouldEqual resourceBody

          secondResult.right.value shouldEqual resourceBody

          verify(lessThanOrExactly(1), postRequestedFor(urlEqualTo(authZeroUri.path)))
        }
      }


      "retry for a new token if the one it has results in a Not Found response" in {
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

        testWithClient(config) { client =>
          val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

          val result = client.expect[String](request).attempt.unsafeRunSync()

          result.right.value shouldEqual resourceBody

          verify(moreThan(1), postRequestedFor(urlEqualTo(authZeroUri.path)))
        }
      }

      "retry for a new token if the one it has results in an Unauthorized response" in {
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

        testWithClient(config) { client =>
          val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

          val result = client.expect[String](request).attempt.unsafeRunSync()

          result.right.value shouldEqual resourceBody

          verify(moreThan(1), postRequestedFor(urlEqualTo(authZeroUri.path)))
        }
      }

      "fail when auth0 is unavailable" in {
        val config = Config(invalidUri, "audience", "client-identity", "client-secret")

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        testWithClient(config) { client =>
          val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

          val result = client.expect[String](request).attempt.unsafeRunSync()

          result.left.value shouldEqual UnexpectedStatus(Status.RequestTimeout)
        }
      }
    }

    "configured without good credentials" should {

      "fail when auth0 responds that they are not authorized" in {
        val config = defaultConfig()

        stubFor(post(urlEqualTo(authZeroUri.path))
          .withRequestBody(equalToJson(authZeroRequestBody(config)))
          .willReturn(unauthorized()))

        stubFor(get(urlEqualTo(resourceUri.path))
          .withHeader(authorizationHeader, equalTo(bearerToken(token)))
          .willReturn(aResponse().withBody(resourceBody)))

        testWithClient(config) { client =>
          val request: Request[IO] = Request(method = Method.GET, uri = resourceUri)

          val result = client.expect[String](request).attempt.unsafeRunSync()

          result.left.value shouldEqual UnexpectedStatus(Status.Unauthorized)
        }
      }
    }
  }

  private val authorizationHeader = "Authorization"

  private val host = "localhost"

  private lazy val baseUri: Uri = Uri.unsafeFromString(s"http://$host:${wireMockServer.port()}/")

  private lazy val resourceUri: Uri = baseUri / "resource" / "sub-resource"

  private lazy val authZeroUri: Uri = baseUri / "oauth" / "token"

  private val invalidUri: Uri = Uri.unsafeFromString(s"http://$host/")

  private val token = "GOOD-TOKEN"

  private val resourceBody = "Hello World"

  private def defaultConfig() = Config(baseUri, "audience", "client-identity", "client-secret")
  private def testWithClient(config: Config = defaultConfig())(test: Http4sClient[IO] => Unit): Unit = {
    val testResult = for {
      httpClient <- BlazeClientBuilder[IO](global).stream
      client = Client(config)(httpClient)
      _ = test(client)
    } yield ()
    testResult.compile.drain.unsafeRunSync()
  }

  private def bearerToken(token: String): String = s"Bearer $token"

  private def authZeroRequestBody(config: Config): String =
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

  override def beforeAll(): Unit = {
    wireMockServer.start()

    configureFor(host, wireMockServer.port())
  }

  override def afterEach(): Unit = wireMockServer.resetAll()

  override def afterAll(): Unit = wireMockServer.stop()
}

