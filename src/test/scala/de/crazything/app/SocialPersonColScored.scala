package de.crazything.app

import de.crazything.search.entity.SearchResult
import play.api.libs.json.{Json, OFormat}

case class SocialPersonColScored(socialPersons: Seq[SearchResult[Int, SocialPerson]])

object SocialPersonColScored {

  implicit def format: OFormat[SocialPersonColScored] = Json.format[SocialPersonColScored]

}
