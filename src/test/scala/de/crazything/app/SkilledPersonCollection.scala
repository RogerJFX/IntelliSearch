package de.crazything.app

import de.crazything.search.entity.SearchResult
import play.api.libs.json.{Json, OFormat}

case class SkilledPersonCollection(socialPersons: Seq[SearchResult[Int, SkilledPerson]])

object SkilledPersonCollection {
  implicit def format: OFormat[SkilledPersonCollection] = Json.format[SkilledPersonCollection]
}
