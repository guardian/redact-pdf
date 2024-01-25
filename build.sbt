
name := """cv-redact-tool"""
organization := "com.gu"

version := "1.0"


lazy val root = (project in file(".")).enablePlugins(PlayScala, JavaServerAppPackaging, SystemdPlugin)


scalaVersion := "3.3.1"
scalacOptions += "-deprecation"


libraryDependencies ++= Seq(
  "org.apache.pdfbox" % "pdfbox" % "2.0.27",
  "com.gu" %% "play-v29-brotli-filter" % "0.15.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
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


/* normalise Debian package name */
val normalisePackageName = taskKey[Unit]("Rename debian package name to be normalised")

normalisePackageName := {
  import Path._

  val targetDirectory = (baseDirectory.value) / "target"
  val debFile = (targetDirectory ** "*.deb").get().head
  val newFile = file(debFile.getParent) / ((Debian / packageName).value + ".deb")

  IO.move(debFile, newFile)
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
    // Remove the PID file
    s"-Dpidfile.path=/dev/null",
)



