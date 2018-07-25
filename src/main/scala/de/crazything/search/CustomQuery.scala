package de.crazything.search

import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._

case object CustomQuery extends QueryConfig {

  case class CQuery(fieldName: String, value: String, boost: Int) {
    def exact: Query = new BoostQuery(new TermQuery(new Term(fieldName, value)), boost)
    def regex: Query = new BoostQuery(new RegexpQuery(new Term(fieldName, value)), boost)
    def phonetic: Query = {
      val parser: QueryParser = new QueryParser(s"$fieldName$PHONETIC", GermanIndexer.phoneticAnalyzer)
      val phoneticQuery = parser.parse(value)
      new BoostQuery(phoneticQuery, boost)
    }
    def fuzzy: Query = new BoostQuery(new FuzzyQuery(new Term(fieldName, value), FUZZY_MAX_EDITS), boost)
  }

  implicit def data2Query(tuple: (String, String, Int)): CQuery = CQuery(tuple._1, tuple._2, tuple._3)
}
