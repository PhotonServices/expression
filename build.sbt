name := """expression"""

version := "0.3.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-contrib" % "2.3.5",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.5",
  "org.scalaz" %% "scalaz-core" % "7.1.0"
)
