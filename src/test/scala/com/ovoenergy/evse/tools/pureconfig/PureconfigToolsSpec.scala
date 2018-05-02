package com.ovoenergy.evse.tools.pureconfig

import com.typesafe.config.ConfigFactory.parseString
import org.http4s.Uri
import org.scalatest.{EitherValues, FlatSpec, Matchers}
import pureconfig._

class PureconfigToolsSpec extends FlatSpec with Matchers with EitherValues {
  "loadConfig" should "read a simple Uri" in {
    case class JustAUri(uri: Uri)

    import PureconfigTools._
    val r = loadConfig[JustAUri](parseString(s"""uri = "http://localhost:9999""""))
    r.right.value shouldBe JustAUri(Uri.uri("http://localhost:9999"))
  }

  it should "fail if the Uri is not well-formed" in {
    case class JustAUri(uri: Uri)

    import PureconfigTools._
    val r = loadConfig[JustAUri](parseString(s"""uri = "http://localhost:99:99""""))
    r shouldBe 'left
  }
}
