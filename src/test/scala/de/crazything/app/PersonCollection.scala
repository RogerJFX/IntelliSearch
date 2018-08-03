package de.crazything.app

import play.api.libs.json.{Json, OFormat}

case class PersonCollection(persons: Seq[Person])

object PersonCollection {
  implicit def format: OFormat[PersonCollection] = Json.format[PersonCollection]
}

