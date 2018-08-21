package de.crazything.app.itest

import de.crazything.app._
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.SearchResult
import de.crazything.search.ext.MappingSearcher
import de.crazything.search.{CommonIndexer, CommonSearcher, DirectoryContainer}
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}

import scala.concurrent.Future
import scala.concurrent.duration._

class DockerTest2 extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with NoLanguage with DirectoryContainer with Network {

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")



  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readSkilledPersons(), SkilledPersonFactory, "skilledIndex")
  }

  "Scala dude" should "should be found" in {
    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))
    CommonSearcher.searchAsync(searchedSkilledPerson, SkilledPersonFactory, searcherOption = "skilledIndex").map(result => {
      println(result)
      assert(result.length == 1)
      assert(result.head.obj.firstName.get == "Roger")
    })
  }

  "Cascaded search" should "find at least one set" in {

    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))

    def combineBaseAndSocialData(skilledPerson: SearchResult[Int, SkilledPerson]): Future[Seq[SearchResult[Int, PersonWithSocialResults]]] = {
      val searchedBasePerson: Person = Person(-1, "", skilledPerson.obj.firstName.getOrElse("-"),
        skilledPerson.obj.lastName.getOrElse("-"), "", "")
      val restResponse: Future[PersonWithSocialPersonsCollection] =
        RestClient.post[Person, PersonWithSocialPersonsCollection](urlFromUriBase("findBaseDataForWithSocial"), searchedBasePerson)
      restResponse.map(res => {
        res.found.map(rr => SearchResult[Int, PersonWithSocialResults](rr, rr.person.score))
      })
    }

    // This one should be moved to API once it is working. Not working at the moment. Very pissed...
    //    def combineBaseAndSocialData(skilledPerson: SearchResult[Int, SkilledPerson]): Future[Seq[SearchResult[Int, PersonWithSocials]]] = {
    //      val searchedBasePerson: Person = Person(-1, "", skilledPerson.obj.firstName.getOrElse("-"),
    //        skilledPerson.obj.lastName.getOrElse("-"), "", "")
    //      val restResponse: Future[PersonCollection] =
    //        RestClient.post[Person, PersonCollection](urlFromUriBase("findBaseDataFor"), searchedBasePerson)
    //
    //      //restResponse.map((res: PersonCollection) => {
    //        val fResult: Future[Seq[SearchResult[Int, PersonWithSocials]]] = for {
    //          person: SearchResult[Int, Person] <- restResponse.map(res => res.persons)
    //          // Tmp. Seems to work somehow, but in the end it is not working. Completely confusing.
    //          //social:Seq[SearchResult[Int, SocialPerson]] = Await.result(combineFacebookScored(person), 5.seconds)
    //          social:Seq[SearchResult[Int, SocialPerson]] <- combineFacebookScored(person)
    //        } yield SearchResult[Int, PersonWithSocials](PersonWithSocials(-1, person, social), 0F)
    //        fResult.map(r => r)
    //      //})
    //    }


    MappingSearcher.searchCombined(input = searchedSkilledPerson, factory = SkilledPersonFactory,
      searcherOption = "skilledIndex",
      combineFn = combineBaseAndSocialData, secondLevelTimeout = 15.seconds)
      .map((result: Seq[(SearchResult[Int, SkilledPerson], Seq[SearchResult[Int, PersonWithSocialResults]])]) => {
        println(result)
        assert(result.head._2.head.obj.socialResults.length == 2)
        assert(result.length == 1)
      })
  }
}
