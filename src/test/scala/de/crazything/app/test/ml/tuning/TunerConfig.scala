package de.crazything.app.test.ml.tuning

import play.api.libs.json.{Json, OFormat}

case class TunerConfig(querySize: Int, threshold: Int = 10, initialBoost: Float = 10F, boostStep: Float = 0.1F)

object TunerConfig {
  implicit def format: OFormat[TunerConfig] = Json.format[TunerConfig]
}