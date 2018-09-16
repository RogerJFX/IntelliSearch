package de.crazything.search

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._

import scala.language.implicitConversions

case object CustomQuery extends QueryConfig {

  /**
    * Query creator.
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
    def wildcard: Query = new BoostQuery(new WildcardQuery(new Term(fieldName, value)), boostOption.getOrElse(Boost.WILDCARD))
    def regex: Query = new BoostQuery(new RegexpQuery(new Term(fieldName, value)), boostOption.getOrElse(Boost.REGEX))
    def phonetic: Query = {
      val parser: QueryParser = new QueryParser(s"$fieldName$PHONETIC_SUFFIX", phoneticAnalyzer)
      val phoneticQuery = parser.parse(value)
      new BoostQuery(phoneticQuery, boostOption.getOrElse(Boost.PHONETIC))
    }
    def fuzzy: Query = new BoostQuery(new FuzzyQuery(new Term(fieldName, value), fuzzyOption.getOrElse(FUZZY_MAX_EDITS)),
      boostOption.getOrElse(Boost.FUZZY))
  }

  case class ConditionQuery(query: Query) {
    def must:(Query, BooleanClause.Occur) = (query, BooleanClause.Occur.MUST)
    def mustNot:(Query, BooleanClause.Occur) = (query, BooleanClause.Occur.MUST_NOT)
    def should:(Query, BooleanClause.Occur) = (query, BooleanClause.Occur.SHOULD)
  }

  implicit def query2ConditionalQuery(q: Query): ConditionQuery = ConditionQuery(q)

  implicit def querySeq2ConditionalQuery(q: Seq[Query]): ConditionQuery = ConditionQuery(q)

  implicit def queryConditionalSeq2ConditionalQuery(q: Seq[(Query, BooleanClause.Occur)]): ConditionQuery = ConditionQuery(q)

  /**
    * Tuple2 to Query.
    * @param tuple fieldName, value
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String))(implicit phoneticAnalyzer: Analyzer): CQuery =
    CQuery(tuple._1, tuple._2)(phoneticAnalyzer)

  /**
    * Tuple3 to Query.
    * @param tuple fieldName, value, boost
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String, Float))(implicit phoneticAnalyzer: Analyzer): CQuery =
    CQuery(tuple._1, tuple._2, Some(tuple._3))(phoneticAnalyzer)

  /**
    * Tuple4 to Query.
    * @param tuple fieldName, value, boost, fuzzyMaxEdits
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String, Float, Int))(implicit phoneticAnalyzer: Analyzer): CQuery =
    CQuery(tuple._1, tuple._2, Some(tuple._3), Some(tuple._4))(phoneticAnalyzer)

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

  /**
    * Creates a BooleanQuery of a Sequence of partial Queries. Just pass something like:
    *
    * {{{
    *   Seq(
    *     (LAST_NAME, person.lastName).exact.should,
    *     (LAST_NAME, person.lastName).phonetic.must,
    *     (LAST_NAME, person.lastName).fuzzy.mustNot
    *   )
    * }}}
    * @param queries Sequence of queries to pass.
    * @return The Boolean query.
    */
  implicit def seq2QueryCondition(queries: Seq[(Query, BooleanClause.Occur)]): BooleanQuery = {
    val queryBuilder = new BooleanQuery.Builder()
    queries.foreach(t => {
      queryBuilder.add(t._1, t._2)
    })
    queryBuilder.build()
  }
}
