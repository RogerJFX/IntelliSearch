package de.crazything.search

case class SearchResult[I, T <: PkDataSet[I]](obj: T, score: Float)
