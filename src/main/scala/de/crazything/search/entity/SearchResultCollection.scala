package de.crazything.search.entity

import play.api.libs.json._

case class SearchResultCollection[I, +T <: PkDataSet[I]](entries: Seq[SearchResult[I, T]])

object SearchResultCollection {

  implicit def format[I, T <: PkDataSet[I]](implicit fmt: Format[SearchResult[I, T]]): OFormat[SearchResultCollection[I, T]] =
    new OFormat[SearchResultCollection[I, T]] {
      def reads(json: JsValue): JsSuccess[SearchResultCollection[I, T]] = JsSuccess(SearchResultCollection[I, T](
        (json \ "entries").as[Seq[SearchResult[I, T]]]
      ))

      def writes(e: SearchResultCollection[I, T]) = JsObject(Seq(
        "entries" -> Json.toJson(e.entries)
      ))
    }

}