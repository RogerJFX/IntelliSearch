package de.crazything.app.test.helpers

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URL
import java.nio.file.{Files, Path, Paths}

import de.crazything.app.{Person, SkilledPerson, SocialPerson}

import scala.collection.mutable.ListBuffer

// Very nice object...
object DataProvider {

  def readVerySimplePersons(): Seq[Person] = {
    import scala.collection.JavaConverters._
    val url: URL = DataProvider.getClass.getResource("/personsVerySimple.txt")
    val path: Path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path).asScala
    val buffer: ListBuffer[Person] = new ListBuffer
    lines.foreach(line => {
      if(!line.startsWith("#")) {
        val arr: Array[String] = line.split(";")
        buffer.append(Person(arr(0).toInt, arr(1), arr(2), arr(3), arr(4), arr(5)))
      }
    })
    buffer
  }

  def readVerySimplePersonsResource(): Seq[Person] = {
    val input: InputStream = DataProvider.getClass.getResourceAsStream("/personsVerySimple.txt")
    //val path: Path = Paths.get(url.toURI)
    val bin = new BufferedReader(new InputStreamReader(input))
    val lines: ListBuffer[String] = ListBuffer()
    var line: String = null
    var end = false
    while(!end) {
      line = bin.readLine()
      if(line == null) end = true
      else lines.append(line)
    }
    val buffer: ListBuffer[Person] = new ListBuffer
    lines.foreach(line => {
      if(!line.startsWith("#")) {
        val arr: Array[String] = line.split(";")
        buffer.append(Person(arr(0).toInt, arr(1), arr(2), arr(3), arr(4), arr(5)))
      }
    })
    buffer
  }

  val noneIfNull:(String) => Option[String] = (str) => if(str == null || str == "null") None else Some(str)

  def readSocialPersons(): Seq[SocialPerson] = {
    import scala.collection.JavaConverters._
    val url: URL = DataProvider.getClass.getResource("/personsSocialVerySimple.txt")
    val path: Path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path).asScala
    val buffer: ListBuffer[SocialPerson] = new ListBuffer
    lines.foreach(line => {
      if(!line.startsWith("#")) {
        val arr: Array[String] = line.split(";")
        buffer.append(SocialPerson(arr(0).toInt, arr(1), arr(2), noneIfNull(arr(3)), noneIfNull(arr(4))))
      }
    })
    buffer
  }

  def readSocialPersonsResource(): Seq[SocialPerson] = {
    val input: InputStream = DataProvider.getClass.getResourceAsStream("/personsSocialVerySimple.txt")
    //val path: Path = Paths.get(url.toURI)
    val bin = new BufferedReader(new InputStreamReader(input))
    val lines: ListBuffer[String] = ListBuffer()
    var line: String = null
    var end = false
    while(!end) {
      line = bin.readLine()
      if(line == null) end = true
      else lines.append(line)
    }
    val buffer: ListBuffer[SocialPerson] = new ListBuffer
    lines.foreach(line => {
      if(!line.startsWith("#")) {
        val arr: Array[String] = line.split(";")
        buffer.append(SocialPerson(arr(0).toInt, arr(1), arr(2), noneIfNull(arr(3)), noneIfNull(arr(4))))
      }
    })
    buffer
  }

  def readSkilledPersons(): Seq[SkilledPerson] = {
    import scala.collection.JavaConverters._
    val url: URL = DataProvider.getClass.getResource("/personsSkillsVerySimple.txt")
    val path: Path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path).asScala
    val buffer: ListBuffer[SkilledPerson] = new ListBuffer
    lines.foreach(line => {
      if(!line.startsWith("#")) {
        val arr: Array[String] = line.split(";")
        val skills = ListBuffer[String]()
        val len = arr.length
        for (i <- 3 until len) {
          skills.append(arr(i))
        }
        buffer.append(SkilledPerson(arr(0).toInt, Some(arr(1)), Some(arr(2)), Some(skills)))
      }
    })
    buffer
  }

  def readSkilledPersonsResource(): Seq[SkilledPerson] = {
    val input: InputStream = DataProvider.getClass.getResourceAsStream("/personsSkillsVerySimple.txt")
    val bin = new BufferedReader(new InputStreamReader(input))
    val lines: ListBuffer[String] = ListBuffer()
    var line: String = null
    var end = false
    while(!end) {
      line = bin.readLine()
      if(line == null) end = true
      else lines.append(line)
    }
    val buffer: ListBuffer[SkilledPerson] = new ListBuffer
    lines.foreach(line => {
      if(!line.startsWith("#")) {
        val arr: Array[String] = line.split(";")
        val skills = ListBuffer[String]()
        val len = arr.length
        for (i <- 3 to len) {
          skills.append(arr(i))
        }
        buffer.append(SkilledPerson(arr(0).toInt, Some(arr(1)), Some(arr(2)), Some(skills)))
      }
    })
    buffer
  }

}
