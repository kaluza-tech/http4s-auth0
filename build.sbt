val catsVersion = "1.1.0"
val circeVersion = "0.9.3"
val fs2Version = "0.10.3"
val http4sVersion = "0.18.10"
val jwtCirceVersion = "0.16.0"
val jwksRsaVersion = "0.4.0"

val wiremockVersion = "2.17.0"
val scalacheckVersion = "1.14.0"
val scalatestVersion = "3.0.5"

lazy val mainDependencies = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "co.fs2" %% "fs2-core" % fs2Version,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "com.pauldijou" %% "jwt-circe" % "0.16.0",
  "com.auth0" % "jwks-rsa" % jwksRsaVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion
)

lazy val testDependencies = Seq(
  "com.github.tomakehurst" % "wiremock" % wiremockVersion,
  "org.scalacheck" %% "scalacheck" % scalacheckVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion,
  "org.slf4j" % "slf4j-nop" % "1.7.22"
).map(_ % "test")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.ovoenergy",
      scalaVersion := "2.12.6"
    )),
    name := "http4s-auth0",
    description := "Client and server middleware for http4s to enable use of Auth0",
    libraryDependencies ++= mainDependencies ++ testDependencies,
    coverageMinimum := 100,
    coverageFailOnMinimum := true,
    credentials += Credentials("Bintray", "dl.bintray.com", sys.env("BINTRAY_USER"), sys.env("BINTRAY_PASS")),
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven",
    fork in Test := true,
    javaOptions in Test += "-Xss64m"
  )
