val catsEffectVersion = "0.10"
val catsVersion = "1.1.0"
val circeVersion = "0.9.3"
val http4sVersion = "0.18.2"

lazy val mainDependencies = Seq(
    "com.auth0" % "jwks-rsa" % "0.3.0",
    "com.github.pureconfig" %% "pureconfig" % "0.8.0",
    "com.pauldijou" %% "jwt-circe" % "0.16.0",
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-generic-extras" % circeVersion,
    "io.circe" %% "circe-java8" % circeVersion,
    "io.circe" %% "circe-literal" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,

//    "org.typelevel" %% "cats-core" % catsVersion,
//    "org.typelevel" %% "cats-effect" % catsEffectVersion,
//    "org.typelevel" %% "cats-free" % catsVersion,

    "org.log4s" %% "log4s" % "1.4.0"
  )


lazy val testDependencies = Seq(
//  "com.github.tomakehurst" % "wiremock" % "2.8.0",
//  "org.scalacheck" % "scalacheck_2.12" % "1.13.5",
  "org.scalatest" %% "scalatest" % "3.0.5"
).map(_ % "test")


lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.ovoenergy",
      scalaVersion := "2.12.4"
    )),
    name := "http4s-auth0",
    description := "Client and server middleware to enable use of Auth0",
    libraryDependencies ++= mainDependencies ++ testDependencies,
    coverageMinimum := 100,
    coverageFailOnMinimum := true,
    // TODO: decide where we releae artifacts to
    resolvers += Resolver.bintrayRepo("ovotech", "maven-private"),
    credentials += Credentials("Bintray", "dl.bintray.com", sys.env("BINTRAY_USER"), sys.env("BINTRAY_PASS")),
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven-private",
    // TODO: find out what licence we should use and include it
    bintrayOmitLicense := true,
    fork in Test := true,
    javaOptions in Test += "-Xss64m"
  )
