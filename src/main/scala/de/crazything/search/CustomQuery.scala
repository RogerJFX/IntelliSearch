package de.crazything.search

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._

import scala.language.implicitConversions

/**
  * Collection of implicit conversions for creating queries and their accumulations most conveniently.
  *
  * Experience of the past let us decide some strange looking things here. Ok, we have a proof of concept here,
  * so stay in touch.
  *
  * E.g. we only have Queries embedded as BoostQueries. This seems odd, and maybe it really is. Falling back to default
  * boost values should once become deprecated. Instead we could simply leave out the boost.
  *
  * Probably we will decide to make phonetic analyzers no more implicit. So this approach might become
  * deprecated very soon.
  */
case object CustomQuery extends QueryConfig {

  /**
    * Query creator.
    *
    * @param fieldName Name of field in search index.
    * @param value value to search.
    * @param boost Custom boost factor. If empty, it will fallback to default boost (see QueryConfig).
    * @param fuzzyDepth Custom fuzzy max edit factor. Only responsible for fuzzy searches. Will fallback
    *                    to default value, if empty (see QueryConfig).
    * @param phoneticAnalyzer The phonetic analyzer.
    */
  private[CustomQuery] case class CQuery(fieldName: String,
                                         value: String,
                                         boost: Option[Float] = None,
                                         fuzzyDepth: Option[Int] = None)
                   (phoneticAnalyzer: Analyzer) {
    def exact: Query = new BoostQuery(new TermQuery(new Term(fieldName, value)), boost.getOrElse(Boost.EXACT))
    def wildcard: Query = new BoostQuery(new WildcardQuery(new Term(fieldName, value)), boost.getOrElse(Boost.WILDCARD))
    def regex: Query = new BoostQuery(new RegexpQuery(new Term(fieldName, value)), boost.getOrElse(Boost.REGEX))
    def phonetic: Query = {

      val parser: QueryParser = new QueryParser(s"$fieldName$PHONETIC_SUFFIX", phoneticAnalyzer)
      val phoneticQuery = parser.parse(value)
      new BoostQuery(phoneticQuery, boost.getOrElse(Boost.PHONETIC))
    }
    def fuzzy: Query = new BoostQuery(new FuzzyQuery(new Term(fieldName, value), fuzzyDepth.getOrElse(FUZZY_MAX_EDITS)),
      boost.getOrElse(Boost.FUZZY))
  }

  /**
    * Normally takes a Query from CQuery. All internal methods produce a tuple2. The tuples will be assembled in
    * method seq2QueryCondition.
    *
    * @param query Query normally obtained from some CQuery method.
    */
  private[CustomQuery] case class ConditionQuery(query: Query) {
    def must:(Query, BooleanClause.Occur) = (query, BooleanClause.Occur.MUST)
    def mustNot:(Query, BooleanClause.Occur) = (query, BooleanClause.Occur.MUST_NOT)
    def should:(Query, BooleanClause.Occur) = (query, BooleanClause.Occur.SHOULD)
  }

  /**
    * Needed internal implicit conversion.
    *
    * @param q A query.
    * @return A ConditionQuery
    */
  implicit def query2ConditionalQuery(q: Query): ConditionQuery = ConditionQuery(q)

  /**
    * Needed internal implicit conversion. Implicitly uses method seq2Query.
    *
    * Grants the option of cascaded query terms like:
    *
    * {{{
    *    Seq(
    *      Seq(
    *        (LAST_NAME, person.lastName).exact,
    *        (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex
    *      ).must,
    *      Seq(
    *        (FIRST_NAME, person.firstName).exact,
    *        (FIRST_NAME, createRegexTerm(person.firstName), Boost.EXACT * 2).regex
    *      ).must
    *    )
    * }}}
    *
    * @param q A sequence of queries.
    * @return A ConditionQuery
    */
  implicit def querySeq2ConditionalQuery(q: Seq[Query]): ConditionQuery = ConditionQuery(q)

  /**
    * Needed internal implicit conversion. Implicitly uses method seq2QueryCondition.
    *
    * Grants the option like method query2ConditionalQuery,
    * but with conditions like must, mustNot, should included. So something like (have a look at the
    * FIRST_NAME section):
    *
    * {{{
    *    Seq(
    *      Seq(
    *        (LAST_NAME, person.lastName).exact,
    *        (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex
    *      ).must,
    *      Seq(
    *        (FIRST_NAME, person.firstName).exact.must,
    *        (FIRST_NAME, createRegexTerm(person.firstName), Boost.EXACT * 2).regex.should
    *      ).must
    *    )
    * }}}
    *
    * @param q A sequence of queries ConditionQuery tuples.
    * @return A ConditionQuery
    */
  implicit def queryConditionalSeq2ConditionalQuery(q: Seq[(Query, BooleanClause.Occur)]): ConditionQuery = ConditionQuery(q)

  /**
    * Tuple2 to Query.
    * @param tuple fieldName, value
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String))(implicit phoneticAnalyzer: Analyzer): CQuery =
    CQuery(fieldName = tuple._1, value = tuple._2)(phoneticAnalyzer)

  /**
    * Tuple3 to Query.
    * @param tuple fieldName, value, boost
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String, Float))(implicit phoneticAnalyzer: Analyzer): CQuery =
    CQuery(fieldName = tuple._1, value = tuple._2, boost = Some(tuple._3))(phoneticAnalyzer)

  /**
    * Tuple4 to Query.
    * @param tuple fieldName, value, boost, fuzzyMaxEdits
    * @return A Query.
    */
  implicit def data2Query(tuple: (String, String, Float, Int))(implicit phoneticAnalyzer: Analyzer): CQuery =
    CQuery(fieldName = tuple._1, value = tuple._2, boost = Some(tuple._3), fuzzyDepth = Some(tuple._4))(phoneticAnalyzer)

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
