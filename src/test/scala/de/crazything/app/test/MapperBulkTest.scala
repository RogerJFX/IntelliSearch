package de.crazything.app.test

import de.crazything.app.NettyRunner
import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.{Person, SocialPerson}
import de.crazything.app.factory.{PersonFactoryDE, SkilledPersonFactory, SocialPersonFactory}
import de.crazything.app.helpers.DataProvider
import de.crazything.search.entity.{Bulk, SearchResult, SearchResultCollection}
import de.crazything.search.ext.MappingSearcher
import de.crazything.search.{CommonIndexer, DirectoryContainer}
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
import play.core.server.NettyServer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class MapperBulkTest extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser
  with GermanLanguage with DirectoryContainer {

  val server: NettyServer = NettyRunner.runServer
  val port: Int = server.httpPort.get

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
    CommonIndexer.index(DataProvider.readSocialPersons(), SocialPersonFactory, "remoteIndex")
    CommonIndexer.index(DataProvider.readSkilledPersons(), SkilledPersonFactory, "skilledIndex")
  }

  override def afterAll: Unit = NettyRunner.stopServer()

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

  def combineFacebookScoredBulk(result: Seq[SearchResult[Int, Person]]): Future[Seq[Seq[SearchResult[Int, SocialPerson]]]] = {
    val persons: Seq[Person] = result.map(r => r.found)
    val restResponse: Future[Bulk[SearchResultCollection[Int, SocialPerson]]] =
      RestClient.post[Bulk[Person], Bulk[SearchResultCollection[Int, SocialPerson]]](urlFromUri("findSocialForScoredBulk"), Bulk(persons))
    restResponse.andThen {
      case Success(res) => println(res)
      case Failure(t) => println(t.getMessage)
    }
    restResponse.map(res => res.entries.map(r => r.entries))
  }

  "Scored remote" should "get a non empty score result for person having facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    MappingSearcher.searchBulk(input = searchedPerson, factory = PersonFactoryDE,
      mapperFn = combineFacebookScoredBulk, secondLevelTimeout = 3.seconds).map(result => {
      println("-------------------")
      println(result)
      assert(result.length == 1)
    })
  }
}
