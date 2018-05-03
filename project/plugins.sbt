// general utility
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.0")

// code quality
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.2.1")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// release
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.1")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")

// TODO: update versions