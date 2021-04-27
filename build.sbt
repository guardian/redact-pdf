name := """redact-pdf"""
organization := "com.gu"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  ws,
  "ai.x" %% "play-json-extensions" % "0.42.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.apache.pdfbox" % "pdfbox" % "2.0.8",
  "fr.opensagres.xdocreport" % "fr.opensagres.poi.xwpf.converter.pdf" % "2.0.2",
  "fr.opensagres.xdocreport" % "xdocreport" % "2.0.2",
  "org.odftoolkit" % "odfdom-java" % "0.8.7",
  "org.seleniumhq.selenium" % "selenium-java" % "3.141.59",
  "com.google.apis" % "google-api-services-sheets" % "v4-rev516-1.23.0",
  "com.google.apis" % "google-api-services-drive" % "v3-rev110-1.23.0",
  "dev.zio" %% "zio" % "1.0.7"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.gu.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.gu.binders._"
