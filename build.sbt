organization := "com.lightbend.akka.johan"
name := "stream-samples"
version := "1.0"
scalaVersion := "2.12.6"
javacOptions += "-Xlint:deprecation"

scalacOptions ++= List(
  "-unchecked",
  "-deprecation"
) 

lazy val AkkaVersion = "2.5.12"
lazy val AkkaHttpVersion = "10.1.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "ch.qos.logback" %  "logback-classic" % "1.1.2"
)

