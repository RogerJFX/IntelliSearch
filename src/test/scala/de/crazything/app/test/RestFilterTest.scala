package de.crazything.app.test

import de.crazything.app.Person._
import de.crazything.app.SocialPersonCollection._
import de.crazything.app._
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.SearchResult
import de.crazything.search.{CommonIndexer, CommonSearcherFiltered}
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
import play.core.server.NettyServer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

class RestFilterTest extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with GermanLanguage {

  //RestClient

  val server: NettyServer = NettyRunner.runServer
  val port: Int = server.httpPort.get

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
    CommonIndexer.index(DataProvider.readSocialPersons(), SocialPersonFactory)
  }
  override def afterAll: Unit = NettyRunner.stopServer()

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

  // Some AKKA property left in configuration. IntelliJ test works, though. Tomorrow...
  def filterHasFacebook(result: SearchResult[Int, Person]): Future[Boolean] = {
    val restResponse: Future[SocialPersonCollection] =
      RestClient.post[Person, SocialPersonCollection](urlFromUri("findSocialFor"), result.obj)
    restResponse.andThen {
      case Success(res) => println(res)
    }
    restResponse.map(res => res.socialPersons.exists(entry => entry.facebookId.isDefined))

  }
//
//  def filterHasFacebook(result: SearchResult[Int, Person]): Future[Boolean] = Future {
//    false
//  }

//  "REST" should "at least work" in {
//    val standardPerson = Person(1, "Herr", "firstName", "lastName", "street", "city")
//    RestClient.get[Person](urlFromUri("foo")).map(response => {
//      assert(response == standardPerson)
//    })
//  }

  "Rest filter" should "get an empty result for missing facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Mayer", "street", "city")
    CommonSearcherFiltered.searchAsyncAsyncFuture (input = searchedPerson, factory = PersonFactoryDE,
      filterFn = filterHasFacebook, filterTimeout = 3.seconds).map(result => {
      assert(result.isEmpty)
    })

  }
}
