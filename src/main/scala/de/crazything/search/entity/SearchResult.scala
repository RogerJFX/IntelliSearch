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

  def $() : Float = score

  def !() : T = found

  /**
    * Failsafe convenient method. Returns one of this or target, if found is a MappedResult and the result itself
    * is wrapped as "target" in MappedResults.
    *
    * It simply is a shortcut for:
    *
    * {{{
    *   searchResult.found.target
    * }}}
    *
    * This method might be called several times in a row. Don't do that, it is just in vain.
    *
    * @tparam I1 Type of origin's PK.
    * @tparam I2 Type of MappedResult's result's PK, if "found" is of type MappedResults.
    * @tparam T1 Type of origin's entity.
    * @tparam T2 Type of MappedResult's entity, if "found" is of type MappedResults.
    * @return The SearchResult, so either this or SearchResult wrapped in MappedResults (there "target").
    */
  def origin[I1 <: I, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]](): SearchResult[I1, T1] = found match {
    case mr: MappedResults[I1, I2, T1, T2] => mr.target
    case _ => this.asInstanceOf[SearchResult[I1, T1]]
  }

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
