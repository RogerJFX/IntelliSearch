package de.crazything.search.entity

case class SearchResult[I, +T <: PkDataSet[I]](obj: T, score: Float)
