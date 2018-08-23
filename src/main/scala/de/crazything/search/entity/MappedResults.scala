package de.crazything.search.entity

import play.api.libs.json._

case class MappedResults[I1, I2, +T1 <: PkDataSet[I1], +T2 <: PkDataSet[I2]](target: SearchResult[I1, T1], results: Seq[SearchResult[I2, T2]])
  extends PkDataSet[I1](target.obj.getId) // Just a placeholder. Target must not be null and will never. results may be empty.

object MappedResults {
  implicit def format[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (implicit fmt1: Format[T1], fmt2: Format[T2]): OFormat[MappedResults[I1, I2, T1, T2]] =
    new OFormat[MappedResults[I1, I2, T1, T2]] {
      override def reads(json: JsValue): JsSuccess[MappedResults[I1, I2, T1, T2]] = JsSuccess(MappedResults[I1, I2, T1, T2](
        (json \ "target").as[SearchResult[I1, T1]],
        (json \ "results").as[Seq[SearchResult[I2, T2]]]
      ))

      override def writes(e: MappedResults[I1, I2, T1, T2]): JsObject = JsObject(Seq(
        "target" -> Json.toJson(e.target),
        "results" -> Json.toJson(e.results)
      ))
    }
}
