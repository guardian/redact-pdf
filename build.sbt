name := """redact-pdf"""
organization := "com.gu"

version := "1.0-SNAPSHOT"
scalacOptions += "-deprecation"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "org.apache.pdfbox" % "pdfbox" % "2.0.18"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.gu.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.gu.binders._"
   