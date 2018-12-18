package de.crazything.search.ml.tuning

import play.api.libs.json.{Json, OFormat}

case class DedicatedTuning(queryIndex: Int,
                           initialBoost: Float,
                           boostStep: Float,
                           minBoost: Float,
                           maxBoost: Float
                          )

object DedicatedTuning {
  implicit def format: OFormat[DedicatedTuning] = Json.format[DedicatedTuning]
}
