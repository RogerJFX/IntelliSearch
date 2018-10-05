package de.crazything.search.entity

import play.api.libs.json._

/**
  * So we have search results of a primary search procedure. We might want to associate results from other services
  * to each of those primary results. This is what this class is about.
  *
  * @param target  Primary search result
  * @param results Results typically from a remote index matching target.
  * @tparam I1 Type of primary key of T1
  * @tparam I2 Type of primary key of T2
  * @tparam T1 Type of primary search result.
  * @tparam T2 Type of second, normally remote result.
  */
case class MappedResults[I1, I2, +T1 <: PkDataSet[I1], +T2 <: PkDataSet[I2]](target: SearchResult[I1, T1],
                                                                             results: Seq[SearchResult[I2, T2]])
  extends PkDataSet[I1](target.found.getId) {

  def :<(): SearchResult[I1, T1] = target

  def !!(): Seq[SearchResult[I2, T2]] = results

  def findMappedResults4Target[I, T <: PkDataSet[I]](targetCondition: MappedQuery,
                                                      resultsCondition: Option[MappedQuery] = None): Option[Seq[SearchResult[I, T]]] = {
    if(targetCondition.filter(Seq(target)).nonEmpty) {
      resultsCondition match {
        case Some(resultFilter) => Some(resultFilter.filter(results.map(r => r.origin()).asInstanceOf[Seq[SearchResult[I, T]]]))
        case None => Some(results.map(r => r.origin()).asInstanceOf[Seq[SearchResult[I, T]]])
      }
    } else {
      //scalastyle:OFF
      // Sorry...
      results.foreach(res => {
        res.findMappedResults4Target(targetCondition, resultsCondition) match {
          case some: Option[Seq[SearchResult[I, T]]] =>
            return some.asInstanceOf[Option[Seq[SearchResult[I, T]]]]
        }
      })
      //scalastyle:ON
      None
    }
  }
}

/**
  * JSON formatting hints.
  */
object MappedResults {

  implicit def format[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (implicit fmt1: Format[T1], fmt2: Format[T2]): OFormat[MappedResults[I1, I2, T1, T2]] =
    new OFormat[MappedResults[I1, I2, T1, T2]] {
      override def reads(json: JsValue): JsSuccess[MappedResults[I1, I2, T1, T2]] =
        JsSuccess(MappedResults[I1, I2, T1, T2](
          (json \ "target").as[SearchResult[I1, T1]],
          (json \ "results").as[Seq[SearchResult[I2, T2]]]
        ))

      override def writes(e: MappedResults[I1, I2, T1, T2]): JsObject =
        JsObject(Seq(
          "target" -> Json.toJson(e.target),
          "results" -> Json.toJson(e.results)
        ))
    }
}
