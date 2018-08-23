package de.crazything.search.entity

import play.api.libs.json._

case class MappedResultsCollection[I1, I2, +T1 <: PkDataSet[I1], +T2 <: PkDataSet[I2]](entries: Seq[MappedResults[I1, I2, T1, T2]])

object MappedResultsCollection {
  implicit def format[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (implicit fmt: Format[MappedResults[I1, I2, T1, T2]]): OFormat[MappedResultsCollection[I1, I2, T1, T2]] =
    new OFormat[MappedResultsCollection[I1, I2, T1, T2]] {
      override def reads(json: JsValue): JsSuccess[MappedResultsCollection[I1, I2, T1, T2]] =
        JsSuccess(MappedResultsCollection[I1, I2, T1, T2](
          (json \ "entries").as[Seq[MappedResults[I1, I2, T1, T2]]]
        ))

      override def writes(e: MappedResultsCollection[I1, I2, T1, T2]): JsObject = JsObject(Seq(
        "entries" -> Json.toJson(e.entries)
      ))
    }
}
