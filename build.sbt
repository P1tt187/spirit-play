
import java.time._
import sbt.Keys._
import sbt._


name := """spirit-play"""

val date = ZonedDateTime.now()

version := date.getYear.toString +"." + (date.getMonth).toString + "." + sys.env.get("BUILD_NUMBER").getOrElse("00")

lazy val scheduleParser = (project in file("subprojects/spirit2-schedule-parser"))

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayAkkaHttpServer, LinuxPlugin, RpmPlugin,JDKPackagerPlugin,SystemdPlugin).enablePlugins(SbtWeb).disablePlugins(PlayNettyServer).enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "meta"
  ).dependsOn(scheduleParser)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  filters,
  cacheApi,
  ehcache,
  ws,
  guice,
  jodaForms,
  "com.typesafe.play" %% "play-json" % "2.6.0",
  "org.webjars" %% "webjars-play" % "2.6.1",
  "org.webjars" % "jquery" % "3.1.1",
  "org.webjars" % "bootstrap" % "3.3.7-1",
  "org.webjars" % "bootstrap-select" % "1.12.0",
  "org.webjars" % "clipboard.js" % "1.6.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.7",
  "jp.co.bizreach" %% "elastic-scala-httpclient" % "2.0.3" withSources(),
  "org.jsoup" % "jsoup" % "1.10.2" % "compile->default" withSources,
  "org.twitter4j" % "twitter4j-core" % "[4.0.4,)",
  "org.mnode.ical4j" % "ical4j" % "2.0.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.1" % Test
).map(_.exclude("io.netty","netty-transport-native-epoll"))

excludeDependencies += "io.netty" % "netty-transport-native-epoll"

resolvers += "twitter4j.org Repository" at "http://twitter4j.org/maven2"
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"


routesGenerator := InjectedRoutesGenerator
//routesGenerator := StaticRoutesGenerator

scalacOptions ++= Seq("-feature", "-language:postfixOps", "-language:implicitConversions")

CoffeeScriptKeys.sourceMap := true

maintainer := "Fabian Markert <f.markert87@gmail.com>"

packageSummary := "Spirit News"

packageDescription := "shows news and schedule"

rpmRelease := Option(sys.props("rpm.buildNumber")) getOrElse "1"

rpmVendor := "http://www.fsi.fh-schmalkalden.de"

rpmUrl := Some("https://github.com/P1tt187/spirit-play")

rpmLicense := Some("None")

rpmGroup := Some("spirit")

//linuxPackageSymlinks := Seq.empty

defaultLinuxLogsLocation := defaultLinuxInstallLocation + "/" + name

rpmBrpJavaRepackJars := false

herokuJdkVersion in Compile := "1.8"

herokuAppName in Compile := "spirit-play"

