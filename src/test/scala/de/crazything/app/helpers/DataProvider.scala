package de.crazything.app.helpers

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URL
import java.nio.file.{Files, Path, Paths}

import de.crazything.app.entity.{Person, SkilledPerson, SocialPerson}

import scala.collection.mutable.ListBuffer

object DataProvider {

  private def doRead[T](urlStr: String, mapperFn: (Array[String]) => T): Seq[T] = {
    import scala.collection.JavaConverters._
    val url: URL = DataProvider.getClass.getResource(urlStr)
    val path: Path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path).asScala
    val buffer: ListBuffer[T] = new ListBuffer
    lines.foreach(line => {
      if(!line.startsWith("#")) {
        val arr: Array[String] = line.split(";")
        buffer.append(mapperFn(arr))
      }
    })
    buffer
  }

  def doReadResource[T](url: String, mapperFn: (Array[String]) => T): Seq[T] = {
    val input: InputStream = DataProvider.getClass.getResourceAsStream(url)
    val bin = new BufferedReader(new InputStreamReader(input))
    val lines: ListBuffer[String] = ListBuffer()
    var line: String = null
    var end = false
    while(!end) {
      line = bin.readLine()
      if(line == null) end = true
      else lines.append(line)
    }
    val buffer: ListBuffer[T] = new ListBuffer
    lines.foreach(line => {
      if(!line.startsWith("#")) {
        val arr: Array[String] = line.split(";")
        buffer.append(mapperFn(arr))
      }
    })
    buffer
  }

  val noneIfNull:(String) => Option[String] = (str) => if(str == null || str == "null") None else Some(str)

  val wrapperFnPerson: (Array[String]) => Person = (arr) => Person(arr(0).toInt, arr(1), arr(2), arr(3), arr(4), arr(5))
  val wrapperFnSocialPerson: (Array[String]) => SocialPerson = (arr) =>
    SocialPerson(arr(0).toInt, arr(1), arr(2), noneIfNull(arr(3)), noneIfNull(arr(4)))
  val wrapperFnSkilledPerson: (Array[String]) => SkilledPerson = (arr) => {
    val skills = ListBuffer[String]()
    val len = arr.length
    for (i <- 3 until len) {
      skills.append(arr(i))
    }
    SkilledPerson(arr(0).toInt, Some(arr(1)), Some(arr(2)), Some(skills))
  }

  def readVerySimplePersons(): Seq[Person] = doRead("/personsVerySimple.txt", wrapperFnPerson)

  def readVerySimplePersonsResource(): Seq[Person] = doReadResource("/personsVerySimple.txt", wrapperFnPerson)


  def readSocialPersons(): Seq[SocialPerson] = doRead("/personsSocialVerySimple.txt", wrapperFnSocialPerson)

  def readSocialPersonsResource(): Seq[SocialPerson] =
    doReadResource("/personsSocialVerySimple.txt", wrapperFnSocialPerson)


  def readSkilledPersons(): Seq[SkilledPerson] = doRead("/personsSkillsVerySimple.txt", wrapperFnSkilledPerson)

  def readSkilledPersonsResource(): Seq[SkilledPerson] =
    doReadResource("/personsSkillsVerySimple.txt", wrapperFnSkilledPerson)


  def readVerySimplePersonsBig(): Seq[Person] = doRead("/persons.txt", wrapperFnPerson)

  def readVerySimplePersonsResourceBig(): Seq[Person] = doReadResource("/persons.txt", wrapperFnPerson)


  def readSocialPersonsBig(): Seq[SocialPerson] = doRead("/personsSocials.txt", wrapperFnSocialPerson)

  def readSocialPersonsResourceBig(): Seq[SocialPerson] =
    doReadResource("/personsSocials.txt", wrapperFnSocialPerson)


  def readSkilledPersonsBig(): Seq[SkilledPerson] = doRead("/personsSkills.txt", wrapperFnSkilledPerson)

  def readSkilledPersonsResourceBig(): Seq[SkilledPerson] =
    doReadResource("/personsSkills.txt", wrapperFnSkilledPerson)

}
