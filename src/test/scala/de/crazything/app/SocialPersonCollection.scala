package de.crazything.app

import de.crazything.search.entity.SearchResult
import play.api.libs.json.{Json, OFormat}

case class SocialPersonCollection(socialPersons: Seq[SearchResult[Int, SocialPerson]])

object SocialPersonCollection {

  implicit def format: OFormat[SocialPersonCollection] = Json.format[SocialPersonCollection]

}
