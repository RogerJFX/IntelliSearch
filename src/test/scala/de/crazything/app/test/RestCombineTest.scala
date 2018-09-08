package de.crazything.app.test

import de.crazything.app.Person._
import de.crazything.app._
import de.crazything.app.test.helpers.{CustomMocks, DataProvider}
import de.crazything.search.entity.{MappedResults, MappedResultsCollection, SearchResult, SearchResultCollection}
import de.crazything.search.{CommonIndexer, CommonSearcher, DirectoryContainer}
import de.crazything.search.ext.MappingSearcher
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
import org.slf4j.{Logger, LoggerFactory}
import play.core.server.NettyServer

import scala.concurrent.duration._
import scala.concurrent.{Future, TimeoutException}
import scala.util.{Failure, Success}


class RestCombineTest extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with GermanLanguage
  with DirectoryContainer with FilterAsync{

  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.RestCombineTest")

  val server: NettyServer = NettyRunner.runServer
  val port: Int = server.httpPort.get

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
    CommonIndexer.index(DataProvider.readSocialPersons(), SocialPersonFactory, "remoteIndex")
    CommonIndexer.index(DataProvider.readSkilledPersons(), SkilledPersonFactory, "skilledIndex")
  }

  override def afterAll: Unit = NettyRunner.stopServer()

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

  def combineFacebookScored(result: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = {
    val restResponse: Future[SearchResultCollection[Int, SocialPerson]] =
      RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUri("findSocialForScored"), result.obj)
    println(result.obj)
    restResponse.andThen {
      case Success(res) => println(res)
      case Failure(t) => println(t.getMessage)
    }
    restResponse.map(res => res.entries)
  }

  def combineFacebookScoredExc(result: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = Future {
    throw new NumberFormatException("Wot up, dude?")
  }

  "Scored remote" should "get a non empty score result for person having facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    MappingSearcher.search(input = searchedPerson, factory = PersonFactoryDE,
      mapperFn = combineFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      println(result)
      assert(result.length == 1)
    })
  }

  it should "get a non mixed score result for person having facebook account" ignore {
    val searchedPerson = Person(-1, "Herr", "Franz", "Rayßer", "street", "city")
    MappingSearcher.search(input = searchedPerson, factory = PersonFactoryDE,
      mapperFn = combineFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      println(result)
      assert(result.length == 2)
      // Mayer was found, but hasn't got a facebook account
      assert(result.head.results.nonEmpty)
      assert(result.head.results.head.obj.facebookId.isEmpty)
    })
  }

  "Combined exc" should "throw an exception if filter does." in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Rayßer", "street", "city")
    recoverToSucceededIf[NumberFormatException](
      MappingSearcher.search(input = searchedPerson, factory = PersonFactoryDE,
        mapperFn = combineFacebookScoredExc).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "work on 4 threads." in {
    val availProcessors = Runtime.getRuntime.availableProcessors()
    def filterAvailProcessors(requested: Int) = if (requested > availProcessors) availProcessors else requested
    val searchedPerson = Person(-1, "Herr", "foobar", "foobar", "street", "city")
    CustomMocks.mockObjectFieldAsync("de.crazything.search.ext.MappingSearcher", "processors", filterAvailProcessors(4), {
      MappingSearcher.search(input = searchedPerson, factory = PersonFactoryAll,
        mapperFn = combineFacebookScored).map(result => {
        assert(result.length == 6)
      })
    })


  }

  it should "throw TimeoutException" in {
    logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    logger.warn("We expect TimeoutException in this test, and RejectedExecutionException as well. So no worries.")
    logger.warn("Reason: we shut down the ThreadPool immediately after a TimeoutException. So the remaining Tasks cannot be executed.")
    logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    recoverToSucceededIf[TimeoutException](
      MappingSearcher.search(input = searchedPerson, factory = PersonFactoryDE,
        mapperFn = combineFacebookScored, secondLevelTimeout = 10.millis).map(result => {
        assert(result.length == 1)
      })
    )
  }

  "Mapping" should "run locally" in {
    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))

    def combineBaseAndSocialData(skilledPerson: SearchResult[Int, SkilledPerson]):
    Future[Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]]] = {
      val searchedBasePerson: Person = Person(-1, "", skilledPerson.obj.firstName.getOrElse("-"),
        skilledPerson.obj.lastName.getOrElse("-"), "", "")
      val restResponse: Future[MappedResultsCollection[Int, Int, Person, SocialPerson]] =
        RestClient.post[Person, MappedResultsCollection[Int, Int, Person, SocialPerson]](
          urlFromUri("mapSocial2Base"), searchedBasePerson)
      val result: Future[Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]]] =
        restResponse.map(res => {
          res.entries.map(rr => SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]](rr, rr.target.score))
        })
      result
    }


    MappingSearcher.search(input = searchedSkilledPerson, factory = SkilledPersonFactory,
      searcherOption = "skilledIndex",
      mapperFn = combineBaseAndSocialData, secondLevelTimeout = 15.seconds)
      .map((result: Seq[MappedResults[Int, Int, SkilledPerson, MappedResults[Int, Int, Person, SocialPerson]]]) => {
        println(result)
        assert(result.head.results.head.obj.results.length == 2)
        assert(result.length == 1)
      })
  }

  it should "throw first level exception" in {
    recoverToSucceededIf[Exception](
      MappingSearcher.search(input = null, factory = SkilledPersonFactory,
        searcherOption = "badIndex",
        mapperFn = null, secondLevelTimeout = 15.seconds)
        .map((result: Seq[MappedResults[Int, Int, SkilledPerson, MappedResults[Int, Int, Person, SocialPerson]]]) => {
          println(result)
          assert(result.head.results.head.obj.results.length == 2)
          assert(result.length == 1)
        })

    )
  }

  "SimpleRemoteSearch" should "work anyway" in {
    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))
    CommonSearcher.searchRemote[Int, SkilledPerson](searchedSkilledPerson, urlFromUri("findSkilledPerson")).map(result => {
      assert(result.length == 1)
    })
  }

  it should "timeout" in {
    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))
    recoverToSucceededIf[TimeoutException](
      CommonSearcher.searchRemote[Int, SkilledPerson](searchedSkilledPerson, urlFromUri("findSkilledPerson"), 2.millis).map(result => {
        assert(result.length == 1)
      })
    )

  }

}
