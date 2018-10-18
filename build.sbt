name := "scala-crdt"

version := "0.1.0"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-distributed-data_2.12" % "2.5.16",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
)
