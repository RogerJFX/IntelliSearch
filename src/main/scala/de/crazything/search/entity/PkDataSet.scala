package de.crazything.search.entity

/**
  * Our strategy is to have sets of data with a primary key. That's because we do not want making a search index
  * a database or whatever.
  *
  * So the real data pool might ly anywhere.
  *
  * If it is a database, the lucene index would give us the primary key's value and we would pass it to the database
  * using some where clause.
  *
  * In the end we have some identifier issue here.
  *
  * @param id id to infer
  * @tparam T Type of id
  */
abstract class PkDataSet[T](id: T) {
  /**
    * We need this one for injecting the id to parent classes. Or maybe not.
    *
    * @return the id
    */
  def getId: T = id
}

