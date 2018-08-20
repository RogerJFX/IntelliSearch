package de.crazything.app.itest

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app._
import de.crazything.search.entity.SearchResult
import de.crazything.search.ext.MappingSearcher
import de.crazything.search.{CommonIndexer, CommonSearcher}
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfter, BeforeAndAfterAll}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._

class DockerTest1 extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with GermanLanguage {

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
  }

  def urlFromUri(uri: String): String = s"http://127.0.0.1:9002/$uri"

  def combineFacebookScored(result: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = {
    val restResponse: Future[SocialPersonColScored] =
      RestClient.post[Person, SocialPersonColScored](urlFromUri("findSocialForScored"), result.obj)
    println(result.obj)
    restResponse.andThen {
      case Success(res) => println(res)
      case Failure(t) => println(t.getMessage)
    }
    restResponse.map(res => res.socialPersons)
  }

  "Scored remote docker" should "get a non empty score result for person having facebook account" in {
    val searchedPerson = Person(-1, "Herr", "Franz", "Reißer", "street", "city")
    MappingSearcher.searchCombined(input = searchedPerson, factory = PersonFactoryDE,
      combineFn = combineFacebookScored, secondLevelTimeout = 5.seconds).map(result => {
      println(result)
      assert(result.length == 1)
    })
  }
}
