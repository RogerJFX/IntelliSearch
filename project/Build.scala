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
      "-language:implicitConversions",
      "-language:postfixOps"
    ),
    resolvers ++= Seq(
      "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

  def assemblySettings = Seq(
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF/services/org.apache.lucene.codecs.Codec") => MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("ignored", xs@_*) => MergeStrategy.discard
      //case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case PathList("application.conf") => MergeStrategy.concat
      case PathList("reference.conf") => MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    test in assembly := {}
  )

//  def assemblyTestSettings = Seq(
//    mainClass in assembly := Some("de.crazything.app.Main"),
//    assemblyMergeStrategy in assembly := {
//      case PathList("org.apache.lucene.codecs.Codec") => MergeStrategy.first
//      case PathList("META-INF", xs@_*) => MergeStrategy.last
//      case PathList("application.conf") => MergeStrategy.concat
//      case PathList("reference.conf") => MergeStrategy.concat
//      case _ => MergeStrategy.first
//    },
//    test in assembly := {},
//    assemblyJarName in assembly := s"${name.value}-test-${version.value}.jar",
//
//    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true, includeDependency = true),
//
//    fullClasspath in assembly := {
//      val cp = (fullClasspath in Test).value
//      cp.filter { file => (file.data.name contains "org.apache.lucene.codecs.Codec") || (file.data.name contains "classes") || (file.data.name contains "test-classes") } ++ (fullClasspath in Runtime).value
//    }
//  )


}
