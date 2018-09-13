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
  * @param found Inner instance
  * @param score Lucene's score estimation of obj
  * @tparam I Type of primary key of type T
  * @tparam T Type of searched object
  */
case class SearchResult[I, +T <: PkDataSet[I]](found: T, score: Float) {

//  def origin[I2, T2]: SearchResult[I, T] = found match {
//    case mr: MappedResults[I, I2, T, T2] => mr.target
//    case _ => this
//  }

  def $ : Float = score

  def ! : T = found

}

/**
  * JSON formatting hints.
  */
object SearchResult {

  implicit def format[I, T <: PkDataSet[I]](implicit fmt: Format[T]): OFormat[SearchResult[I, T]] =
    new OFormat[SearchResult[I, T]] {
      def reads(json: JsValue): JsSuccess[SearchResult[I, T]] = JsSuccess(SearchResult[I, T](
        (json \ "found").as[T],
        (json \ "score").as[Float]
      ))

      def writes(e: SearchResult[I, T]) = JsObject(Seq(
        "found" -> Json.toJson(e.found),
        "score" -> Json.toJson(e.score)
      ))
    }



}
