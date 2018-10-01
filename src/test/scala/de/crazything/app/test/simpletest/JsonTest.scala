package de.crazything.app.test.simpletest

import de.crazything.app.entity.Person._
import de.crazything.app.entity.Person
import de.crazything.service.QuickJsonParser
import org.scalatest.FlatSpec
import play.api.libs.json._

// TODO: may be removed later
class JsonTest extends FlatSpec with QuickJsonParser{

  case class PersonCollection(persons: Seq[Person])

  object PersonCollection {
    implicit def format: OFormat[PersonCollection] = Json.format[PersonCollection]
  }

  val standardPersonJsonString = """{"id":1,"salutation":"Herr","firstName":"firstName","lastName":"lastName","street":"street","city":"city"}"""
  val standardPerson = Person(1, "Herr", "firstName", "lastName", "street", "city")

  "Foo" should "bar" in {
    println(t2JsonString[Person](standardPerson))
    assert(true)
  }

  it should "baz" in {
    println(jsonString2T[Person](standardPersonJsonString))
    assert(true)
  }

  it should "qux" in {
    println(t2JsonString[PersonCollection](PersonCollection(List(standardPerson, standardPerson))))
    assert(true)
  }

  it should "quux" in {
    println(jsonString2T[PersonCollection]("""{"persons":[{"id":1,"salutation":"Herr","firstName":"firstName","lastName":"lastName","street":"street","city":"city"},{"id":1,"salutation":"Herr","firstName":"firstName","lastName":"lastName","street":"street","city":"city"}]}"""))
    assert(true)
  }

}
