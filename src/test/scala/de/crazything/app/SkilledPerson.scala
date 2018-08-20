package de.crazything.app

import de.crazything.search.entity.PkDataSet
import play.api.libs.json.{Json, OFormat}

case class SkilledPerson(id: Int, firstName: Option[String],
                         lastName: Option[String], skills: Option[Seq[String]] = None /*no skills... :D*/)
  extends PkDataSet[Int](id)


object SkilledPerson {

  implicit def format: OFormat[SkilledPerson] = Json.format[SkilledPerson]

}