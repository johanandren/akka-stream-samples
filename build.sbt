organization := "com.lightbend.akka.johan"
name := "stream-samples"
version := "1.0"
scalaVersion := "2.11.8"

scalacOptions ++= List(
  "-unchecked",
  "-deprecation"
) 

lazy val AkkaVersion = "2.4.10"
lazy val AkkaStreamContribVersion = "0.3"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http-jackson-experimental" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-contrib" % AkkaStreamContribVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.11",
  "ch.qos.logback" %  "logback-classic" % "1.1.2"

)

