name := """reactive-scala"""

version := "1.1"

scalaVersion := "2.12.3"

val akkaVersion = "2.5.4"
val akkaHttpVersion = "10.0.10"
val sprayVersion = "1.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "org.iq80.leveldb"  % "leveldb" % "0.9",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.scalactic" %% "scalactic" % "3.0.4",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "io.spray" %%  "spray-json" % sprayVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10" )

resolvers ++= Seq(
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/")