package de.crazything.search

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._

case object CustomQuery extends QueryConfig {
  /**
    * Query creator.
    *
    *
    *
    * @param fieldName Name of field in search index.
    * @param value value to search.
    * @param boostOption Custom boost factor. If empty, it will fallback to default boost (see QueryConfig).
    * @param fuzzyOption Custom fuzzy max edit factor. Only responsible for fuzzy searches. Will fallback
    *                    to default value, if empty (see QueryConfig).
    * @param phoneticAnalyzer The phonetic analyzer.
    */
  case class CQuery(fieldName: String, value: String, boostOption: Option[Float] = None,
                                         fuzzyOption: Option[Int] = None)
                   (phoneticAnalyzer: Analyzer) {
    def exact: Query = new BoostQuery(new TermQuery(new Term(fieldName, value)), boostOption.getOrElse(Boost.EXACT))
    def regex: Query = new BoostQuery(new RegexpQuery(new Term(fieldName, value)), boostOption.getOrElse(Boost.REGEX))
    def phonetic: Query = {
      val parser: QueryParser = new QueryParser(s"$fieldName$PHONETIC_SUFFIX", phoneticAnalyzer)
      val phoneticQuery = parser.parse(value)
      new BoostQuery(phoneticQuery, boostOption.getOrElse(Boost.PHONETIC))
    }
    def fuzzy: Query = new BoostQuery(new FuzzyQuery(new Term(fieldName, value), fuzzyOption.getOrElse(FUZZY_MAX_EDITS)),
      boostOption.getOrElse(Boost.FUZZY))
  }

  /**
    * Tuple2 to Query.
    * @param tuple fieldName, value
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String))(implicit analyzer: Analyzer): CQuery =
    CQuery(tuple._1, tuple._2)(analyzer)

  /**
    * Tuple3 to Query.
    * @param tuple fieldName, value, boost
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String, Int))(implicit analyzer: Analyzer): CQuery =
    CQuery(tuple._1, tuple._2, Some(tuple._3))(analyzer)

  /**
    * Tuple4 to Query.
    * @param tuple fieldName, value, boost, fuzzyMaxEdits
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String, Int, Int))(implicit analyzer: Analyzer): CQuery =
    CQuery(tuple._1, tuple._2, Some(tuple._3), Some(tuple._4))(analyzer)

  /**
    * Creates a BooleanQuery of a Sequence of partial Queries. Just pass something like:
    *
    * {{{
    *   Seq(
    *     (LAST_NAME, person.lastName).exact,
    *     (LAST_NAME, person.lastName).phonetic
    *   )
    * }}}
    *
    * and you should get what you need.
    *
    * @param queries Sequence of queries to pass.
    * @return The Boolean query.
    */
  implicit def seq2Query(queries: Seq[Query]): BooleanQuery = {
    val queryBuilder = new BooleanQuery.Builder()
    queries.foreach(query => {
      queryBuilder.add(query, BooleanClause.Occur.SHOULD)
    })
    queryBuilder.build()
  }
}
