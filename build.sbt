name := """expression"""

version := "0.1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-contrib" % "2.3.5",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.5"
)
