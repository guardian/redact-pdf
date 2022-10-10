
name := """cv-redact-tool"""
organization := "com.gu"

version := "1.0"


lazy val root = (project in file(".")).enablePlugins(PlayScala, JavaServerAppPackaging)


scalaVersion := "2.13.8"
scalacOptions += "-deprecation"


libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "org.apache.pdfbox" % "pdfbox" % "2.0.18"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.gu.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.gu.binders._"

/* 
   Packaging settings section

   We are using sbt-native-packager to build a debian package.
   The SBT settings below are used for the build of that package
 */

/* use package name without version and `_all` */
Debian / packageName := normalizedName.value

Debian / packageBin := {
  val originalFile = (Debian / packageBin).value
  val newFile = file(originalFile.getParent) / (packageName + ".deb")
  IO.move(originalFile, newFile)
  newFile
}


/* A debian package needs some mandatory settings to be valid */
maintainer := "The Guardian engineering managers  <engineering.managers@theguardian.com>"
Debian / packageSummary := "Online web app to redact cv"
Debian / packageDescription := "Online web app to redact cv"

/* While not mandatory it is still highly recommended to add relevant JRE package as a dependency */ 
Debian / debianPackageDependencies := Seq("java11-runtime-headless")

/* Configure the Java options with which the executable will be launched */
Universal / javaOptions ++= Seq(
    // -J params will be added as jvm parameters
    "-J-Xmx2g",
    "-J-Xms2g",
)



