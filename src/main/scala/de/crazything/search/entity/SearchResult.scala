package de.crazything.search.entity

import play.api.libs.json.{Json, OFormat}

case class SearchResult[I, +T <: PkDataSet[I]](obj: T, score: Float)

//object SearchResult {
//  implicit def format[I, T <: PkDataSet[I]]: OFormat[SearchResult[I, T]] = Json.format[SearchResult[I, T]]
//}
