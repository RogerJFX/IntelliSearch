package de.crazything.app.test

import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search.{AbstractTypeFactory, CommonIndexer, CommonSearcherFiltered, QueryConfig}
import org.apache.lucene.document.Document
import org.apache.lucene.search.Query
import org.scalatest.{AsyncFlatSpec, BeforeAndAfter}

import scala.concurrent.{ExecutionContext, Future, Promise}

class FilterAsyncTest extends AsyncFlatSpec with BeforeAndAfter with QueryConfig with GermanLanguage {

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  private def filterFrankfurt(result: SearchResult[Int, Person]): Boolean = result.obj.city.contains("Frankfurt")

  private def filterFrankfurtAsync(result: SearchResult[Int, Person]): Future[Boolean] = Future {
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
  // Make sure async processing does not destroy order of results.
  "Results" should "be sorted properly" in {
    object PersonFactoryAll extends AbstractTypeFactory[Int, Person] {

      override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = PersonFactoryDE.createInstanceFromDocument(doc)

      override def setDataPool(data: Seq[Person]): Unit = ???

      override def populateDocument(document: Document, dataSet: Person): Unit = ???

      override def createQuery(t: Person): Query = {
        import de.crazything.search.CustomQuery.{data2Query, seq2Query}
        Seq(
          ("lastName", "Hösl").exact,
          ("firstName", "Fr*").wildcard,
          ("lastName", ".*").regex,
          ("lastName", "Mayer").phonetic)
      }

      override def selectQueryCreator: (QueryCriteria, Person) => Query = ???
    }

//    def filterTrueOLD(result: SearchResult[Int, Person]): Future[Boolean] = Future {
//      val timeout: Long = scala.util.Random.nextInt(500).toLong
//      Thread.sleep(timeout)
//      println(timeout)
//      println(result)
//      true
//    }

    // Foo
    def filterTrue(result: SearchResult[Int, Person]): Future[Boolean] = {
      val promise: Promise[Boolean] = Promise[Boolean]

      Future.apply({
        val timeout: Long = scala.util.Random.nextInt(500).toLong
        Thread.sleep(timeout)
        println(timeout)
        println(result)
        promise.success(true)
      })
      println(s"Future method $result")
      promise.future
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

    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrue).map(result => {
      checkOrder(result)
      assert(result.length == 6)
    })
  }

}
