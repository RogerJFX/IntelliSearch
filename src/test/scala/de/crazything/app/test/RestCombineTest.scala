package de.crazything.app.test

import de.crazything.app.Person._
import de.crazything.app.SocialPersonCollection._
import de.crazything.app._
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.SearchResult
import de.crazything.search.CommonIndexer
import de.crazything.search.ext.MappingSearcher
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
import org.slf4j.{Logger, LoggerFactory}
import play.core.server.NettyServer

import scala.concurrent.duration._
import scala.concurrent.{Future, TimeoutException}
import scala.util.{Failure, Success}

class RestCombineTest extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with GermanLanguage {

  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.RestCombineTest")

  val server: NettyServer = NettyRunner.runServer
  val port: Int = server.httpPort.get

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
    CommonIndexer.index(DataProvider.readSocialPersons(), SocialPersonFactory, "remoteIndex")
  }

  override def afterAll: Unit = NettyRunner.stopServer()

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

  def combineFacebookScored(result: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = {
    val restResponse: Future[SocialPersonCollection] =
      RestClient.post[Person, SocialPersonCollection](urlFromUri("findSocialForScored"), result.obj)
    println(result.obj)
    restResponse.andThen {
      case Success(res) => println(res)
      case Failure(t) => println(t.getMessage)
    }
    restResponse.map(res => res.socialPersons)
  }

  def combineFacebookScoredExc(result: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = Future {
    throw new NumberFormatException("Wot up, dude?")
  }

  "Scored remote" should "get a non empty score result for person having facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    MappingSearcher.searchCombined(input = searchedPerson, factory = PersonFactoryDE,
      combineFn = combineFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      println(result)
      assert(result.length == 1)
    })
  }

  it should "get a non mixed score result for person having facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Rayßer", "street", "city")
    MappingSearcher.searchCombined(input = searchedPerson, factory = PersonFactoryDE,
      combineFn = combineFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      println(result)
      assert(result.length == 2)
      // Mayer was found, but hasn't got a facebook account
      assert(result.head._2.nonEmpty)
      assert(result.head._2.head.obj.facebookId.isEmpty)
    })
  }

  "Combined exc" should "throw an exception if filter does." in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Rayßer", "street", "city")
    recoverToSucceededIf[NumberFormatException](
      MappingSearcher.searchCombined(input = searchedPerson, factory = PersonFactoryDE,
        combineFn = combineFacebookScoredExc).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "throw TimeoutException" in {
    logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    logger.warn("We expect TimeoutException in this test, and RejectedExecutionException as well. So no worries.")
    logger.warn("Reason: we shut down the ThreadPool immediately after a TimeoutException. So the remaining Tasks cannot be executed.")
    logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    recoverToSucceededIf[TimeoutException](
      MappingSearcher.searchCombined(input = searchedPerson, factory = PersonFactoryDE,
        combineFn = combineFacebookScored, secondLevelTimeout = 10.millis).map(result => {
        assert(result.length == 1)
      })
    )
  }
}
