package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search._
import org.apache.lucene.document.Document
import org.apache.lucene.search.{BooleanClause, BooleanQuery, Query}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfter, Matchers}

class FilterAsyncTest extends AsyncFlatSpec with Matchers with BeforeAndAfter with QueryConfig with GermanLanguage {

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  private def filterFrankfurt(result: SearchResult[Int, Person]): Boolean = result.obj.city.contains("Frankfurt")

  private def filterFrankfurtAsync(result: SearchResult[Int, Person]): Boolean = {
    Thread.sleep(500) // Come on! Just half a second...
    filterFrankfurt(result)
  }


  before {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
  }

  "Async search with sync filter" should "exclude Mayer living not in Frankfurt" in {
    CommonSearcherFiltered.searchAsync(input = standardPerson.copy(lastName = "Mayer"), factory = PersonFactoryDE,
      filterFn = filterFrankfurt).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "pass Hösl living in Frankfurt" in {
    CommonSearcherFiltered.searchAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
      filterFn = filterFrankfurt).map(result => {
      assert(result.length == 1)
    })
  }

  "Async search with async filter" should "exclude Mayer living not in Frankfurt" in {
    import scala.concurrent.duration._
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson.copy(lastName = "Mayer"), factory = PersonFactoryDE,
      filterFn = filterFrankfurtAsync, filterTimeout = 10.seconds).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "pass Hösl living in Frankfurt" in {
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
      filterFn = filterFrankfurtAsync).map(result => {
      assert(result.length == 1)
    })
  }

  object PersonFactoryAll extends AbstractTypeFactory[Int, Person] {

    import de.crazything.search.CustomQuery._

    override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = PersonFactoryDE.createInstanceFromDocument(doc)

    override def setDataPool(data: Seq[Person]): Unit = ???

    override def populateDocument(document: Document, dataSet: Person): Unit = ???

    override def createQuery(t: Person): Query = {

      Seq(
        ("lastName", "Hösl").exact,
        ("firstName", "Fr*").wildcard,
        ("lastName", ".*").regex,
        ("lastName", "Mayer").phonetic)
    }

    override def selectQueryCreator: (QueryCriteria, Person) => Query = (criteria, person) => {
      if(criteria.queryName == "dummy") {
        Seq(
          ("firstName", "Roger").exact.must,
          ("lastName", person.lastName).exact.must
        )
      } else createQuery(person)
    }
  }

  "Combined searches" should "get only the Author" in {

    def filterRoger(result: SearchResult[Int, Person]): Boolean = {
      // normally another Factory/Directory - just a check on some other data source
      CommonSearcher.search(input = standardPerson.copy(lastName = result.obj.lastName, firstName="Roger"),
        factory = PersonFactoryAll, queryCriteria = Some(QueryCriteria("dummy"))).nonEmpty
    }

    import scala.concurrent.duration._
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterRoger, filterTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })

  }

  // Make sure async processing does not destroy order of results.
  "Results" should "be sorted async (quickly)" in {

    def filterTrue(result: SearchResult[Int, Person]): Boolean = {
      val timeout: Long = scala.util.Random.nextInt(500).toLong + 500L
      try {
        Thread.sleep(timeout)
      } catch {
        case _: Exception => // ignore
      }

      true
    }

    def checkOrder(seq: Seq[SearchResult[Int, Person]]): Unit = {
      def check(head: SearchResult[Int, Person], tail: Seq[SearchResult[Int, Person]]): Unit = {
        val nextHead = tail.head
        assert(head.score >= nextHead.score)
        val nextTail = tail.tail
        if (nextTail.nonEmpty) {
          check(nextHead, nextTail)
        }
      }

      check(seq.head, seq.tail)
    }

    import scala.concurrent.duration._
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrue, filterTimeout = 10.seconds).map(result => {
      checkOrder(result)
      assert(result.length == 6)
    })


  }

  // Make sure async processing does not destroy order of results.
  it should "be sorted sync (slowly)" in {

    def filterTrue(result: SearchResult[Int, Person]): Boolean = {
      val timeout: Long = scala.util.Random.nextInt(500).toLong + 500L
      try {
        Thread.sleep(timeout)
      } catch {
        case _: Exception => // ignore
      }

      true
    }

    def checkOrder(seq: Seq[SearchResult[Int, Person]]): Unit = {
      def check(head: SearchResult[Int, Person], tail: Seq[SearchResult[Int, Person]]): Unit = {
        val nextHead = tail.head
        assert(head.score >= nextHead.score)
        val nextTail = tail.tail
        if (nextTail.nonEmpty) {
          check(nextHead, nextTail)
        }
      }

      check(seq.head, seq.tail)
    }

    CommonSearcherFiltered.searchAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrue).map(result => {
      checkOrder(result)
      assert(result.length == 6)
    })


  }


}
