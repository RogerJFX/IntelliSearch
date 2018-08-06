package de.crazything.app

import play.api.libs.json.{Json, OFormat}


case class SocialPersonCollection(socialPersons: Seq[SocialPerson])

object SocialPersonCollection {

  implicit def format: OFormat[SocialPersonCollection] = Json.format[SocialPersonCollection]
  //implicit def formatP: OFormat[SocialPerson] = SocialPerson.format
}
