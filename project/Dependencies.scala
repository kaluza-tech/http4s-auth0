import sbt._

object Dependencies {

  private val http4sVersion = "0.18.2"
  private val catsVersion = "1.0.1"
  private val circeVersion = "0.9.2"

  lazy val akkaVersion = "2.4.20"
  lazy val kafkaVersion = "0.11.0.0"
  lazy val kafkaSerializationVersion = "0.3.8"

  lazy val shared = Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "co.fs2" %% "fs2-core" % "0.10.2",
    "io.circe" %% "circe-generic" % circeVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion
  )

  lazy val test = Seq(
    "org.scalatest" %% "scalatest" % "3.0.4",
    "com.github.tomakehurst" % "wiremock" % "2.8.0"
  ).map(_ % "test")

  lazy val serviceDependencies = shared ++ Seq(
    "org.typelevel" %% "cats-free" % catsVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-java8" % circeVersion,
    "com.ovoenergy" %% "evse-messaging-client" % "0.0.8",
    "com.github.pureconfig" %% "pureconfig" % "0.8.0",
    "com.beachape" %% "enumeratum" % "1.5.13",
    "com.beachape" %% "enumeratum-circe" % "1.5.16",
    "com.pauldijou" %% "jwt-circe" % "0.16.0",
    "com.auth0" % "jwks-rsa" % "0.3.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.log4s" %% "log4s" % "1.4.0",
    "com.internetitem" % "logback-elasticsearch-appender" % "1.6",
    "com.microsoft.azure.sdk.iot" % "iot-service-client" % "1.13.0",
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.18",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.apache.kafka" % "kafka-clients" % kafkaVersion,
    "com.ovoenergy" %% "kafka-serialization-core" % kafkaSerializationVersion,
    "com.ovoenergy" %% "kafka-serialization-avro4s" % kafkaSerializationVersion
  )

  lazy val serviceTestDependencies = test ++ Seq(
    "org.mockito" % "mockito-core" % "1.8.5",
    "net.cakesolutions" %% "scala-kafka-client-testkit" % "1.0.0"
  ).map(_ % "test")

  lazy val clientDependencies = shared ++ Seq(
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "io.circe" %% "circe-generic-extras" % circeVersion,
    "io.circe" %% "circe-literal" % circeVersion
  )

  lazy val clientTestDependencies = test ++ Seq(
    "org.scalacheck" % "scalacheck_2.12" % "1.13.5"
  ).map(_ % "test")
}
