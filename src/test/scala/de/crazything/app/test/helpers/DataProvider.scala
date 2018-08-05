package de.crazything.app.test.helpers

import java.net.URL
import java.nio.file.{Files, Path, Paths}

import de.crazything.app.{Person, SocialPerson}

import scala.collection.mutable.ListBuffer

object DataProvider {

  def readVerySimplePersons(): Seq[Person] = {
    import scala.collection.JavaConverters._
    val url: URL = DataProvider.getClass.getResource("/personsVerySimple.txt")
    val path: Path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path).asScala
    val buffer: ListBuffer[Person] = new ListBuffer
    lines.foreach(line => {
      val arr: Array[String] = line.split(";")
      buffer.append(Person(arr(0).toInt, arr(1), arr(2), arr(3), arr(4), arr(5)))
    })
    buffer
  }

  val noneIfNull:(String) => Option[String] = (str) => if(str == "null") None else Some(str)

  def readSocialPersons(): Seq[SocialPerson] = {
    import scala.collection.JavaConverters._
    val url: URL = DataProvider.getClass.getResource("/personsSocial.txt")
    val path: Path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path).asScala
    val buffer: ListBuffer[SocialPerson] = new ListBuffer
    lines.foreach(line => {
      val arr: Array[String] = line.split(";")
      buffer.append(SocialPerson(arr(0).toInt, arr(1), arr(2), noneIfNull(arr(3)), noneIfNull(arr(4))))
    })
    buffer
  }

}
