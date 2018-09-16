package de.crazything.app

import de.crazything.app.PersonFactoryDE.{FIRST_NAME, LAST_NAME}
import de.crazything.search.CustomQuery._
import de.crazything.search.QueryConfig
import org.apache.lucene.search.{BooleanClause, BooleanQuery, Query}

// We need GermanLanguage here, since it declares an implicit phonetic analyzer.
trait PersonQueries extends QueryConfig with GermanLanguage with GermanRegexReplace {
  /**
    * The normal method.
    *
    * We have the chance to pass a custom boost factor and a custom fuzzyMaxEdit (aka Levenstein range).
    *
    * The only thing we can't do here in comparison is disabling queries be passing a filter. Normally we
    * don't need to do this since this is a factory class.
    *
    * @param person Our search object
    * @return The desired BooleanQuery
    */
  def doCreateStandardQuery(person: Person): Query = {
    Seq(
      (LAST_NAME, person.lastName).exact.should,
      (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex.should,
      (LAST_NAME, person.lastName, Boost.PHONETIC).phonetic.should,
      (LAST_NAME, person.lastName, Boost.FUZZY, FUZZY_MAX_EDITS).fuzzy.should
    )

  }

  def doCreateCascadedStandardQuery(person: Person): Query =
    Seq(
      Seq(
        (LAST_NAME, person.lastName).exact,
        (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex
      ).must,
      Seq(
        (FIRST_NAME, person.firstName).exact.must,
        (FIRST_NAME, createRegexTerm(person.firstName), Boost.EXACT * 2).regex.should
      ).must
    )


  def createFirstAndLastNameQuery(person: Person): Query = {
    val result: Seq[Query] =
      Seq(
        (LAST_NAME, person.lastName).exact,
        (LAST_NAME, createRegexTerm(person.lastName), Boost.EXACT * 20).regex,
        (LAST_NAME, person.lastName, Boost.PHONETIC).phonetic,

        (FIRST_NAME, person.firstName).exact,
        //    (FIRST_NAME, createWildCardTerm(person.firstName), Boost.WILDCARD / 1.5F).wildcard,
        (FIRST_NAME, createRegexTerm(person.firstName), Boost.EXACT * 2).regex,
        (FIRST_NAME, person.firstName, Boost.PHONETIC / 2F).phonetic
      )
    result
  }

  /**
    * Custom method. We have a param queryEnableOpt here. So if we pass Some(QueryEnabled.REGEX), only the RegexQuery
    * will perform.
    *
    * Maybe deprecated in some later version.
    *
    * @param person         Our search object
    * @param queryEnableOpt Selection of enabled types of Query.
    * @return Normally a BooleanQuery
    */
  def createSuperCustomQuery(person: Person, queryEnableOpt: Option[Int] = Some(QueryEnabled.ALL)): Query = {

    val queryEnable = queryEnableOpt.get // cannot be empty

    val queryBuilder = new BooleanQuery.Builder()

    if (checkEnabled(queryEnable, QueryEnabled.EXACT)) {
      // OR customized: queryBuilder.add((LAST_NAME, person.lastName, 10000).exact, BooleanClause.Occur.SHOULD) -> Boost 10000
      queryBuilder.add((LAST_NAME, person.lastName).exact, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.REGEX)) {
      queryBuilder.add((LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.PHONETIC)) {
      queryBuilder.add((LAST_NAME, person.lastName, Boost.PHONETIC).phonetic, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.FUZZY)) {
      // OR without maxEdits: queryBuilder.add((LAST_NAME, person.lastName, Boost.FUZZY).fuzzy, BooleanClause.Occur.SHOULD)
      // In this case FUZZY_MAX_EDITS would be fetched as default from QueryConfig
      queryBuilder.add((LAST_NAME, person.lastName, Boost.FUZZY, FUZZY_MAX_EDITS).fuzzy, BooleanClause.Occur.SHOULD)
    }

    queryBuilder.build()

  }

  // "Hans-Peter", "Hans Peter" will become "Hans*"
  // "Hans" remains "Hans"
  private def createWildCardTerm(in: String): String = {
    val tokens = in.split("[ -]*?")
    if (tokens.length > 1) {
      s"${tokens.head}*"
    } else {
      in
    }
  }

  private[this] def checkEnabled(queryOr: Int, queryEnabled: Int): Boolean = (queryOr & queryEnabled) == queryEnabled
}
