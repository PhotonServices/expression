name := """expression"""

version := "0.1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-contrib" % "2.3.5",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.5",
  "io.spray" %% "spray-can" % "1.3.2",
  "io.spray" %% "spray-client" % "1.3.2",
  "io.spray" %% "spray-routing" % "1.3.2",
  "io.spray" %% "spray-json" % "1.3.0",
  "org.scalatestplus" %% "play" % "1.1.0" % "test",
  "org.scalaz" %% "scalaz-core" % "7.1.0"
)

scalacOptions ++= Seq(
  "-deprecation"
)

