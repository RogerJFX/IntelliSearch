package de.crazything.search.ml.guard

import play.api.libs.json.{Json, OFormat}

case class GuardConfig(maxClickedAs: Int = 25, maxPosition: Int = 100, minTimeGap: Long = 500, maxTimeFouls: Int = 10)

object GuardConfig {
  implicit def format: OFormat[GuardConfig] = Json.format[GuardConfig]
}
