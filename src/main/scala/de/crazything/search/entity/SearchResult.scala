package de.crazything.search.entity

import play.api.libs.json._

case class SearchResult[I, +T <: PkDataSet[I]](obj: T, score: Float)

object SearchResult {

  implicit def format[I, T <: PkDataSet[I]](implicit fmt: Format[T]): OFormat[SearchResult[I, T]] =
    new OFormat[SearchResult[I, T]] {
      def reads(json: JsValue): JsSuccess[SearchResult[I, T]] = JsSuccess(SearchResult[I, T](
        (json \ "obj").as[T],
        (json \ "score").as[Float]
      ))

      def writes(e: SearchResult[I, T]) = JsObject(Seq(
        "obj" -> Json.toJson(e.obj),
        "score" -> Json.toJson(e.score)
      ))
    }

}
