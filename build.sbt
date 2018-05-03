val catsVersion = "1.0.1"
val circeVersion = "0.9.2"
val fs2Version = "0.10.2"
val http4sVersion = "0.18.2"


// TODO: make imports minimal
lazy val mainDependencies = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "co.fs2" %% "fs2-core" % fs2Version,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
//  "org.typelevel" %% "cats-free" % catsVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
//  "io.circe" %% "circe-parser" % circeVersion,
//  "io.circe" %% "circe-java8" % circeVersion,
//  "com.github.pureconfig" %% "pureconfig" % "0.8.0",
//  "com.beachape" %% "enumeratum" % "1.5.13",
//  "com.beachape" %% "enumeratum-circe" % "1.5.16",
  "com.pauldijou" %% "jwt-circe" % "0.16.0",
  "com.auth0" % "jwks-rsa" % "0.3.0",
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion
)

lazy val testDependencies = Seq(
  "com.github.tomakehurst" % "wiremock" % "2.8.0",
  "org.scalacheck" % "scalacheck_2.12" % "1.13.5",
  "org.scalatest" %% "scalatest" % "3.0.5"
).map(_ % "test")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.ovoenergy",
      scalaVersion := "2.12.3"
    )),
    name := "http4s-auth0",
    description := "Client and server middleware for http4s to enable use of Auth0",
    libraryDependencies ++= mainDependencies ++ testDependencies,
    coverageMinimum := 100,
    coverageFailOnMinimum := true,
    // TODO: decide where we releae artifacts to
    resolvers += Resolver.bintrayRepo("ovotech", "maven-private"),
    credentials += Credentials("Bintray", "dl.bintray.com", sys.env("BINTRAY_USER"), sys.env("BINTRAY_PASS")),
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven",
    // TODO: find out what licence we should use and include it
    bintrayOmitLicense := true,
    fork in Test := true,
    javaOptions in Test += "-Xss64m"
  )
