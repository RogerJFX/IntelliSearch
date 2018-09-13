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

  "BiggerData test" should "even work with generic DTO class" in {

    type FinalResult = Seq[MappedResults[Int, Int, SkilledPerson, MappedResults[Int, Int, Person, SocialPerson]]]

    type CombineResult = MappedResultsCollection[Int, Int, Person, SocialPerson]

    // OR:
    // type CombineResult = Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]]

    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Ecmascript", "Postgres", "Scala", "Linux", "Java")))

    implicit def skill2Base(skilledPerson: SearchResult[Int, SkilledPerson]): Person =
      Person(-1, "", skilledPerson.found.firstName.getOrElse("-"), skilledPerson.found.lastName.getOrElse("-"), "", "")


    def combineBaseAndSocialData(skilledPerson: SearchResult[Int, SkilledPerson]): Future[CombineResult] =
      RestClient.post[Person, MappedResultsCollection[Int, Int, Person, SocialPerson]](
        urlFromUriBase("mapSocial2BaseBig"), skilledPerson)


    MappingSearcher.search(input = searchedSkilledPerson, factory = dataFactory, maxHits = 4,
      mapperFn = combineBaseAndSocialData, secondLevelTimeout = 4.minutes)
      .map((result: FinalResult) => {
        println(result)
        println("------------")
        val firstSkilledPerson: SkilledPerson = result.head :< () found
        val firstHitMappings: Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]] = result.head !! ()
        val firstPerson: Person = firstHitMappings.head.found.target.found

        val firstPersonSocialHits: Seq[SearchResult[Int, SocialPerson]] = firstHitMappings.head ! () results

        val firstPersonSocialHitScore: Float = firstHitMappings.head $()

        val firstSocialPerson: SocialPerson = firstPersonSocialHits.head ! ()

        result.foreach(sp => println(sp.target))
        result.head.results.foreach(sp => println(sp.found.target))
        println(s"Found skilled person is: $firstSkilledPerson")
        println(s"Found base person is: $firstPerson")
        println(s"Found social person is: $firstSocialPerson")
        assert(firstPersonSocialHitScore == 489.59003F)
        assert(firstSkilledPerson.firstName.get == "Burchard")
        assert(firstSkilledPerson.lastName.get == "Stoeckl")
        assert(firstPerson.firstName == "Burkhard")
        assert(firstPerson.lastName == "Stöckl")
        assert(firstPersonSocialHits.length == 20)
        assert(firstSocialPerson.firstName == "Burchard")
        assert(firstSocialPerson.lastName == "Stoeckl")
        assert(result.length == 4)
      })
  }

}
