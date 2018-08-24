package de.crazything.search.entity

/**
  * Simple case class to let search factories be a bit smarter.
  *
  * Any factory has a default queries. To enable others, we use this helper class.
  *
  * There is an option to modify custom queries as well. See param queryEnableOpt.
  *
  * @param queryName Name of alternative query.
  * @param queryEnableOpt bitwise flag mask as option. It is possible to exclude e.g. the fuzzy query by passing
  *                       a mask without QueryEnabled.FUZZY, though the query with name `queryName` normally uses
  *                       fuzzy query. So it is a very late bound option.
  */
case class QueryCriteria(queryName: String, queryEnableOpt: Option[Int] = None)
