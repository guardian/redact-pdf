addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.20")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

/* 
   The following is needed because scala-xml has not be updated to 2.x in sbt yet but has in sbt-native-packager 
   See: https://github.com/scala/bug/issues/12632
 */
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
