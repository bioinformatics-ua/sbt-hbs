//import bintray.Keys._

sbtPlugin := true

organization := "com.bicou.sbt"

name := "sbt-hbs"

version := "1.0.8"

scalaVersion := "2.10.4"

resolvers += Resolver.typesafeRepo("releases")

libraryDependencies ++= Seq(
  "org.webjars" % "mkdirp" % "0.5.0"
)

addSbtPlugin("com.typesafe.sbt" %% "sbt-js-engine" % "1.0.2")

//publishMavenStyle := false

//bintraySettings

//repository in bintray := "sbt-plugins"

//bintrayOrganization in bintray := None

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scriptedSettings

//scriptedBufferLog := false

//scriptedLaunchOpts += ("-Dproject.version=" + version.value )

publishTo <<= version { v: String =>
  val nexus = "https://dev.bmd-software.com/nexus/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("Public (Snapshots)" at nexus + "content/repositories/snapshots")
  else
    Some("Public (Releases)" at nexus + "content/repositories/releases")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")