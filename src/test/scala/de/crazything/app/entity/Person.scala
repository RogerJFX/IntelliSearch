package de.crazything.app.entity

import de.crazything.search.entity.PkDataSet
import play.api.libs.json._

case class Person(id: Int, salutation: String, firstName: String, lastName: String, street: String, city: String)
  extends PkDataSet[Int](id)

object Person {
  implicit def format: OFormat[Person] = Json.format[Person]
}