package de.crazything.app.test.ml

import de.crazything.search.entity.PkDataSet
import play.api.libs.json.{Json, OFormat}

case class Slogan(id: Int, firstName: String, lastName: String, slogan1: String, slogan2: String, slogan3: String)
  extends PkDataSet[Int](id)

object Slogan {
  implicit def format: OFormat[Slogan] = Json.format[Slogan]
}