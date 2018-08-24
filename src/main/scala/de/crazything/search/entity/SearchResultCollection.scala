package de.crazything.search.entity

import play.api.libs.json._

/**
  * This class is needed for remoting, so at last JSON serialization. We simply put a sequence of SearchResults here.
  *
  * @param entries Sequence of SearchResult. See there.
  * @tparam I Type of primary key of T, so the search result
  * @tparam T Type of search result
  */
case class SearchResultCollection[I, +T <: PkDataSet[I]](entries: Seq[SearchResult[I, T]])

/**
  * JSON formatting hints.
  */
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