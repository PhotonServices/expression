name := """expression"""

version := "0.3.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.2"

libraryDependencies ++= {
  val akkaV = "2.3.6" 
Seq(
  "com.typesafe.akka" %% "akka-cluster" % akkaV,
  "com.typesafe.akka" %% "akka-testkit" % akkaV,
  "com.typesafe.akka" %% "akka-contrib" % akkaV,
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaV,
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "org.scalaz" %% "scalaz-core" % "7.1.0"
)}
