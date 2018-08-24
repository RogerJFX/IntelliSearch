package de.crazything.app.test

import de.crazything.app.Person._
import de.crazything.app._
import de.crazything.app.test.helpers.DataProvider
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

//  def filterHasFacebook(result: SearchResult[Int, Person]): Future[Boolean] = {
//    val restResponse: Future[SocialPersonCollection] =
//      RestClient.post[Person, SocialPersonCollection](urlFromUri("findSocialFor"), result.obj)
//    restResponse.andThen {
//      case Success(res) => println(res)
//    }
//    restResponse.map(res => res.socialPersons.exists(entry => entry.facebookId.isDefined))
//  }

//  def filterHasFacebookScored(result: SearchResult[Int, Person]): Future[Boolean] = {
//    val restResponse: Future[SocialPersonCollection] =
//      RestClient.post[Person, SocialPersonCollection](urlFromUri("findSocialForScored"), result.obj)
//    restResponse.andThen {
//      case Success(res) => println(res)
//    }
//    restResponse.map(res => res.socialPersons.exists(entry => entry.obj.facebookId.isDefined && entry.score > 20F))
//  }

  def filterHasFacebookScored(result: SearchResult[Int, Person]): Future[Boolean] = {
    val restResponse: Future[SearchResultCollection[Int, SocialPerson]] =
      RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUri("findSocialForScored"), result.obj)
    restResponse.andThen {
      case Success(res) => println(res)
    }
    restResponse.map(res => res.entries.exists(entry => entry.obj.facebookId.isDefined && entry.score > 20F))
  }

  "Rest filter" should "get an empty result for missing facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Mayer", "street", "city")
    FilteringSearcher.searchAsyncAsyncFuture(input = searchedPerson, factory = PersonFactoryDE,
      filterFn = filterHasFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "get a non empty result for person having facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    FilteringSearcher.searchAsyncAsyncFuture(input = searchedPerson, factory = PersonFactoryDE,
      filterFn = filterHasFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })
  }

  it should "get a non empty result - or write it like this (demo)." in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    FilteringSearcher.searchAsyncAsyncFuture(input = searchedPerson, factory = PersonFactoryDE,
      filterFn = (result: SearchResult[Int, Person]) => {
        RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUri("findSocialForScored"), result.obj)
          .map(res => res.entries.exists(entry => entry.obj.facebookId.isDefined))
      }, secondLevelTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })
  }

  "Scored remote" should "get a non empty score result for person having facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    FilteringSearcher.searchAsyncAsyncFuture(input = searchedPerson, factory = PersonFactoryDE,
      filterFn = filterHasFacebookScored, secondLevelTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })
  }
}
