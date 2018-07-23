package de.crazything.app

import de.crazything.search._
import org.apache.lucene.document._
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._

object PersonFactory extends AbstractTypeFactory[Int, Person] with FieldModify with QueryConfig {

  val SALUTATION = "salutation"
  val FIRST_NAME = "firstName"
  val LAST_NAME = "lastName"
  val STREET = "street"
  val CITY = "city"

  val PHONETIC = "PHON"

  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = {
    DataContainer.findById(doc.get("id").toInt)
  }

  override def setDataPool(data: Seq[Person]): Unit = {
    DataContainer.setData(data)
  }

  // TODO: do we really need new extra fields for phonetic search? Maybe later?
  override def populateDocument(document: Document, dataSet: Person): Unit = {

    document.add(new StoredField("id", dataSet.id))

    document.add(new Field(SALUTATION, prepareField(dataSet.salutation), StringField.TYPE_NOT_STORED))
    document.add(new Field(FIRST_NAME, prepareField(dataSet.firstName), StringField.TYPE_NOT_STORED))
    document.add(new Field(LAST_NAME, prepareField(dataSet.lastName), StringField.TYPE_NOT_STORED))
    document.add(new Field(STREET, prepareField(dataSet.street), StringField.TYPE_NOT_STORED))
    document.add(new Field(CITY, prepareField(dataSet.city), StringField.TYPE_NOT_STORED))

    document.add(new Field(s"$SALUTATION$PHONETIC", prepareField(dataSet.salutation), TextField.TYPE_NOT_STORED))
    document.add(new Field(s"$FIRST_NAME$PHONETIC", prepareField(dataSet.firstName), TextField.TYPE_NOT_STORED))
    document.add(new Field(s"$LAST_NAME$PHONETIC", prepareField(dataSet.lastName), TextField.TYPE_NOT_STORED))
    document.add(new Field(s"$STREET$PHONETIC", prepareField(dataSet.street), TextField.TYPE_NOT_STORED))
    document.add(new Field(s"$CITY$PHONETIC", prepareField(dataSet.city), TextField.TYPE_NOT_STORED))

  }

  /**
    * TMP!
    *
    * Mayer, Maier, Meyer should match Meier. Even Majr.
    */
  val createRegexTMP: (String) => String = (input) => {
    val occurs = Seq("ei", "ai", "ey", "ay")
    val replacement = "(a|e)(i|j|y)e?"
    occurs.foldLeft(input)((r, c) => r.replace(c, replacement))
  }

  /*
   TODO: we should work this out.
   Maybe some implicits.
    */
  override def createQuery(t: Person, queryEnable: Int = QueryEnabled.ALL): Query = {

    val parser: QueryParser = new QueryParser(s"$LAST_NAME$PHONETIC", GermanIndexer.phoneticAnalyzer)
    val phoneticQuery = parser.parse(prepareField(t.lastName))

    val queryBuilder = new BooleanQuery.Builder()

    if ((queryEnable & QueryEnabled.TERM) == QueryEnabled.TERM)
      queryBuilder.add(new BoostQuery(new TermQuery(new Term(LAST_NAME, prepareField(t.lastName))), Boost.TERM),
        BooleanClause.Occur.SHOULD)

    if ((queryEnable & QueryEnabled.REGEX) == QueryEnabled.REGEX)
      queryBuilder.add(new BoostQuery(new RegexpQuery(new Term(LAST_NAME, createRegexTMP(t.lastName))), Boost.REGEX),
        BooleanClause.Occur.SHOULD)

    if ((queryEnable & QueryEnabled.PHONETIC) == QueryEnabled.PHONETIC)
      queryBuilder.add(new BoostQuery(phoneticQuery,Boost.PHONETIC), BooleanClause.Occur.SHOULD)

    if ((queryEnable & QueryEnabled.FUZZY) == QueryEnabled.FUZZY)
      queryBuilder.add(new BoostQuery(new FuzzyQuery(new Term(LAST_NAME, prepareField(t.lastName)), FUZZY_MAX_EDITS), Boost.FUZZY),
        BooleanClause.Occur.SHOULD)

    queryBuilder.build()

    //    new BooleanQuery.Builder() // ???
    //      // Boost perfect matches by 10
    //      .add(new BoostQuery(new TermQuery(new Term(LAST_NAME, prepareField(t.lastName))), 10), BooleanClause.Occur.SHOULD)
    //      // Custom Regex mods. Boost it by 5
    //      .add(new BoostQuery(new RegexpQuery(new Term(LAST_NAME, createRegexTMP(t.lastName))), 5), BooleanClause.Occur.SHOULD)
    //      // Last but one option, maybe boosted later by 2
    //      .add(phoneticQuery, BooleanClause.Occur.SHOULD)
    //      // Last option should be FuzzyQuery.
    //      .build()

  }

}
