import sbt.Keys.{test, _}
import sbt.{Setting, enablePlugins, taskKey, _}
import sbtassembly.AssemblyPlugin.autoImport.MergeStrategy
import sbtassembly.{AssemblyKeys, PathList}
import sbtdocker.DockerPlugin
import sbtdocker.DockerPlugin.autoImport.{Dockerfile, docker, dockerfile}

import scala.sys.process._

object CustomTasks extends AssemblyKeys {

  val intern1Port = "9001"
  val intern2Port = "9002"

  lazy val intTest = taskKey[Unit]("Only run dockerized tests")

  lazy val runDocker = taskKey[Unit]("Execute shell commands")

  val dockerTasks: Seq[Setting[_]] = Seq[Setting[_]](
    runDocker := {
      "sh runDocker.sh" !
    },
    intTest := {
      (testOnly in Test).toTask(s" de.crazything.app.itest.*").value
    }
  )

  def assemblyTestSettings = Seq(
    mainClass in assembly := Some("de.crazything.app.Main"),
    assemblyMergeStrategy in assembly := {
      //case PathList("META-INF/services/org.apache.lucene.codecs.Codec") => MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      //case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case PathList("application.conf") => MergeStrategy.concat
      case PathList("reference.conf") => MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-test-${version.value}.jar",

    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true, includeDependency = true),

    fullClasspath in assembly := {
      val cp = (fullClasspath in Test).value
      cp.filter { file => (file.data.name contains "org.apache.lucene.codecs.Codec") || (file.data.name contains "classes") || (file.data.name contains "test-classes") } ++ (fullClasspath in Runtime).value
    }
  )

  enablePlugins(DockerPlugin)

  import sbt.File

  lazy val dockerSettings1 = Seq(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = s"/app1/${artifact.getName}"

      new Dockerfile {
        from("openjdk:8-jre")
        add(artifact, artifactTargetPath)
        copy(new File("src/test/resources/personsSocial.txt"), "/app1/data.txt", "daemon:daemon")
        expose(9001)
        cmd("java", "-jar", artifactTargetPath, intern1Port)
      }
    }
  )

  lazy val dockerSettings2 = Seq(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = s"/app2/${artifact.getName}"

      new Dockerfile {
        from("openjdk:8-jre")
        add(artifact, artifactTargetPath)
        copy(new File("src/test/resources/personsSocial.txt"), "/app2/data.txt", "daemon:daemon")
        expose(9002)
        cmd("java", "-jar", artifactTargetPath, intern2Port)
      }
    }
  )

  dockerfile in docker := {
    val artifact: File = assembly.value
    val artifactTargetPath = s"/app/${artifact.getName}"

    new Dockerfile {
      from("openjdk:8-jre")
      add(artifact, artifactTargetPath)
      copy(new File("src/test/resources/personsSocial.txt"), "/app/data.txt", "daemon:daemon")
      entryPoint("java", "-jar", artifactTargetPath)
    }
  }

}
