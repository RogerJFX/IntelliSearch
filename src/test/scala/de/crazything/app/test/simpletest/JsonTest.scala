package de.crazything.app.test.simpletest

import de.crazything.app.entity.Person._
import de.crazything.app.entity.Person
import de.crazything.service.QuickJsonParser
import org.scalatest.FlatSpec
import play.api.libs.json._

class JsonTest extends FlatSpec with QuickJsonParser{

  case class PersonCollection(persons: Seq[Person])

  object PersonCollection {
    implicit def format: OFormat[PersonCollection] = Json.format[PersonCollection]
  }

  val standardPersonJsonString = """{"id":1,"salutation":"Herr","firstName":"firstName","lastName":"lastName","street":"street","city":"city"}"""
  val standardPerson = Person(1, "Herr", "firstName", "lastName", "street", "city")

  val standardPersonCollectionJsonString = """{"persons":[{"id":1,"salutation":"Herr","firstName":"firstName","lastName":"lastName","street":"street","city":"city"},{"id":1,"salutation":"Herr","firstName":"firstName","lastName":"lastName","street":"street","city":"city"}]}"""
  val standardPersonCollection = PersonCollection(List(standardPerson, standardPerson))

  "Foo" should "bar" in {
    val bar = t2JsonString[Person](standardPerson)
    assert(bar == standardPersonJsonString)
  }

  it should "baz" in {
    val baz = jsonString2T[Person](standardPersonJsonString)
    assert(baz == standardPerson)
  }

  it should "qux" in {
    val qux = t2JsonString[PersonCollection](standardPersonCollection)
    assert(qux == standardPersonCollectionJsonString)
  }

  it should "quux" in {
    val quux = jsonString2T[PersonCollection](standardPersonCollectionJsonString)
    assert(quux == standardPersonCollection)
  }

}
