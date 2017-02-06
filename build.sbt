organization := "com.lightbend.akka.johan"
name := "stream-samples"
version := "1.0"
scalaVersion := "2.12.2"

scalacOptions ++= List(
  "-unchecked",
  "-deprecation"
) 

lazy val AkkaVersion = "2.4.16"
lazy val AkkaHttpVersion = "10.0.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "ch.qos.logback" %  "logback-classic" % "1.1.2"
)

