package de.crazything.app.itest

import de.crazything.app._
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.{MappedResults, MappedResultsCollection, SearchResult}
import de.crazything.search.ext.MappingSearcher
import de.crazything.search.{CommonIndexer, DirectoryContainer}
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}

import scala.concurrent.Future
import scala.concurrent.duration._

class DockerTestBigger extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with NoLanguage
  with DirectoryContainer with Network {

  private val dataFactory = new SkilledPersonFactory()

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readSkilledPersonsBig(), dataFactory)
  }

  // Does not work so far. So tmp. ignored.
  "BiggerData test" should "even work with generic DTO class" ignore {

    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala")))

    def combineBaseAndSocialData(skilledPerson: SearchResult[Int, SkilledPerson]):
    Future[Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]]] = {
      val searchedBasePerson: Person = Person(-1, "", skilledPerson.obj.firstName.getOrElse("-"),
        skilledPerson.obj.lastName.getOrElse("-"), "", "")
      val restResponse: Future[MappedResultsCollection[Int, Int, Person, SocialPerson]] =
        RestClient.post[Person, MappedResultsCollection[Int, Int, Person, SocialPerson]](
          urlFromUriBase("mapSocial2BaseBig"), searchedBasePerson)
      val result: Future[Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]]] =
        restResponse.map(res => {
          res.entries.map(rr => SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]](rr, rr.target.score))
        })
      result
    }


    MappingSearcher.search(input = searchedSkilledPerson, factory = dataFactory,
      mapperFn = combineBaseAndSocialData, secondLevelTimeout = 15.seconds)
      .map((result: Seq[MappedResults[Int, Int, SkilledPerson, MappedResults[Int, Int, Person, SocialPerson]]]) => {
        println(result)
        assert(result.head.results.head.obj.results.length == 2)
        assert(result.length == 1)
      })
  }

}
