name := """expression"""

version := "0.3.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-contrib" % "2.3.5",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.5",
  "org.mongodb" %% "casbah" % "2.7.4",
  "org.scalatestplus" %% "play" % "1.1.0" % "test",
  "org.scalaz" %% "scalaz-core" % "7.1.0"
)
