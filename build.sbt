
name := "soracle"

version := "0.1"

scalaVersion := "2.12.6"

lazy val root = (project in file("."))
  .settings(Build.projectSettings)
  .settings(Build.assemblySettings)

lazy val integration1 = Project("Int1", file("modules/int1"))
  .dependsOn(root % "compile->compile;test->test")
  .aggregate(root)
  .settings(Build.projectSettings)
  .settings(CustomTasks.assemblyTestSettings)
  .enablePlugins(DockerPlugin)
  .settings(CustomTasks.dockerSettings1)

lazy val integration2 = Project("Int2", file("modules/int2"))
  .dependsOn(root % "compile->compile;test->test")
  .aggregate(root)
  .settings(Build.projectSettings)
  .settings(CustomTasks.assemblyTestSettings)
  .enablePlugins(DockerPlugin)
  .settings(CustomTasks.dockerSettings2)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "7.4.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "7.4.0",
  "org.apache.lucene" % "lucene-queryparser" % "7.4.0",
  "org.apache.lucene" % "lucene-analyzers-phonetic" % "7.4.0",
  "commons-codec" % "commons-codec" % "1.11",

  "com.typesafe.play" %% "play-netty-server" % "2.6.13",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.9",

  "com.typesafe.scala-logging" % "scala-logging-slf4j_2.11" % "2.1.2",
  "ch.qos.logback" % "logback-classic" % "1.1.7",

  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test

)

fork in test := true

parallelExecution in Test := false

lazy val unitTest = taskKey[Unit]("Only run unit tests excluding docker tests")

unitTest := {
  (testOnly in Test).toTask(s" de.crazything.app.test.*").value
}

import CustomTasks.{intTest, _}

dockerTasks

TaskKey[Unit]("dockerize") := {
  Def.sequential(
    docker in integration1,
    docker in integration2,
    runDocker,
    intTest
  ).value

  println("Rerun integration test with sbt intTest")
}
