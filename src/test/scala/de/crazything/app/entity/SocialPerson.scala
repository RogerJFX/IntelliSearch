package de.crazything.app.entity

import de.crazything.search.entity.PkDataSet
import play.api.libs.json.{Json, OFormat}

case class SocialPerson(id: Int, firstName: String, lastName: String, facebookId: Option[String] = None, twitterId: Option[String] = None)
  extends PkDataSet[Int](id)

object SocialPerson {
  implicit def format: OFormat[SocialPerson] = Json.format[SocialPerson]
}
