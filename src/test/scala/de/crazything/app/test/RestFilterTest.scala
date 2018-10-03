package de.crazything.app.test

import de.crazything.app.entity.Person._
import de.crazything.app._
import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.{Person, SocialPerson}
import de.crazything.app.factory.{PersonFactoryDE, SocialPersonFactory}
import de.crazything.app.helpers.DataProvider
import de.crazything.search.CommonIndexer
import de.crazything.search.entity.{SearchResult, SearchResultCollection}
import de.crazything.search.ext.FilteringSearcher
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
import play.core.server.NettyServer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

class RestFilterTest extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with GermanLanguage {

  val server: NettyServer = NettyRunner.runServer
  val port: Int = server.httpPort.get

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
    CommonIndexer.index(DataProvider.readSocialPersons(), SocialPersonFactory, "remoteIndex")
  }

  override def afterAll: Unit = NettyRunner.stopServer()

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

  def filterHasFacebookScored(result: SearchResult[Int, Person]): Future[Boolean] = {
    val restResponse: Future[SearchResultCollection[Int, SocialPerson]] =
      RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUri("findSocialForScored"), result.found)
    restResponse.andThen {
      case Success(res) => println(res)
    }
    restResponse.map(res => res.entries.exists(entry => entry.found.facebookId.isDefined && entry.score > 20F))
  }

  "Rest filter" should "get an empty result for missing facebook account" ignore {
    val searchedPerson = Person(-1, "Herr", "Idiot", "Dumpwater", "street", "city")
    FilteringSearcher.search(input = searchedPerson, factory = PersonFactoryDE,
      filterFn = filterHasFacebookScored, secondLevelTimeout = 3.minutes).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "get a non empty result for person having facebook account" ignore {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    FilteringSearcher.search(input = searchedPerson, factory = PersonFactoryDE,
      filterFn = filterHasFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })
  }

  it should "get a non empty result - or write it like this (demo)." ignore  {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    FilteringSearcher.search(input = searchedPerson, factory = PersonFactoryDE,
      filterFn = (result: SearchResult[Int, Person]) => {
        RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUri("findSocialForScored"), result.found)
          .map(res => res.entries.exists(entry => entry.found.facebookId.isDefined))
      }, secondLevelTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })
  }

  "Scored remote" should "get a non empty score result for person having facebook account" ignore  {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    FilteringSearcher.search(input = searchedPerson, factory = PersonFactoryDE,
      filterFn = filterHasFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })
  }
}
