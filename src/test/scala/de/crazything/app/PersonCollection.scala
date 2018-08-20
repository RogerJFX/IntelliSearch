package de.crazything.app

import de.crazything.search.entity.SearchResult
import play.api.libs.json.{Json, OFormat}

case class PersonCollection(persons: Seq[SearchResult[Int, Person]])

object PersonCollection {
  implicit def format: OFormat[PersonCollection] = Json.format[PersonCollection]
}
