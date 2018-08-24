package de.crazything.search.entity

import play.api.libs.json._

/**
  *
  * Any search result has an inner instance and a score estimated by Lucene.
  *
  * Any inner instance has a primary key.
  *
  * The inner instance is from type T, the inner instance's primary key is from type I.
  *
  * @param obj Inner instance
  * @param score Lucene's score estimation of obj
  * @tparam I Type of primary key of type T
  * @tparam T Type of searched object
  */
case class SearchResult[I, +T <: PkDataSet[I]](obj: T, score: Float)

/**
  * JSON formatting hints.
  */
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
