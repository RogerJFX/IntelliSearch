import sbt.Keys.{test, _}
import sbt.{Resolver, _}
import sbtassembly.AssemblyPlugin.autoImport.MergeStrategy
import sbtassembly.{AssemblyKeys, PathList}

object Build extends AssemblyKeys {
  def projectSettings = Seq(
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions"
    ),
    resolvers ++= Seq(
      "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

  def assemblySettings = Seq(
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case PathList("application.conf") => MergeStrategy.concat
      case PathList("reference.conf") => MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    test in assembly := {}
  )
}
