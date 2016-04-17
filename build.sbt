name := """spirit-play"""

version := "2.0.0-SNAPSHOT"

lazy val scheduleParser = (project in file("subprojects/spirit2-schedule-parser"))

lazy val root = (project in file(".")).enablePlugins(PlayScala).enablePlugins(SbtWeb).enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "meta"
  ).dependsOn(scheduleParser).aggregate(scheduleParser)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(

  cache,
  ws,
  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars" % "jquery" % "2.2.2",
  "org.webjars" % "bootstrap" % "3.3.6",
  "org.webjars" % "bootstrap-select" % "1.9.4",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.2",
  "jp.co.bizreach" %% "elastic-scala-httpclient" % "1.0.5" withSources(),
  "org.jsoup" % "jsoup" % "1.8.3" % "compile->default" withSources,
  "org.twitter4j" % "twitter4j-core" % "4.0.4",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.0" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//routesGenerator := InjectedRoutesGenerator
routesGenerator := StaticRoutesGenerator

scalacOptions ++= Seq("-feature", "-language:postfixOps", "-language:implicitConversions")

CoffeeScriptKeys.sourceMap := true
