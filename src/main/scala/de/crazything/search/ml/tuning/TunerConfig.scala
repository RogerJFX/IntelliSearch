package de.crazything.search.ml.tuning

import play.api.libs.json.{Json, OFormat}

case class TunerConfig(querySize: Int,
                       threshold: Int = 10,
                       initialBoost: Float = 10F,
                       boostStep: Float = 0.1F,
                       minBoost: Float = 20F,
                       maxBoost: Float = 1F,
                       dedicated: Seq[DedicatedTuning]
                        )

object TunerConfig {
  implicit def format: OFormat[TunerConfig] = Json.format[TunerConfig]
}
