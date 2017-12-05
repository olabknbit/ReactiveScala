//import io.gatling.sbt.GatlingPlugin

name := """reactive-scala"""

version := "1.1"

scalaVersion := "2.12.3"

val akkaVersion = "2.5.4"
val akkaHttpVersion = "10.0.10"
val sprayVersion = "1.3.3"

//enablePlugins(GatlingPlugin)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0" % "test",
  "io.gatling" % "gatling-core" % "2.3.0",
  "io.gatling" % "gatling-http" % "2.3.0",
  "io.gatling" % "gatling-test-framework" % "2.3.0" % "test",
  "io.spray" %% "spray-json" % sprayVersion,
  "net.liftweb" %% "lift-json" % "3.2.0-M3",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.iq80.leveldb" % "leveldb" % "0.9",
  "org.scalactic" %% "scalactic" % "3.0.4",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test")

resolvers ++= Seq(
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

//addSbtPlugin("io.gatling" % "gatling-sbt" % "2.2.0")