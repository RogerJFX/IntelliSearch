package de.crazything.app.itest

import de.crazything.app._
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.{CommonIndexer, CommonSearcher}
import de.crazything.search.entity.{PkDataSet, SearchResult}
import de.crazything.search.ext.MappingSearcher
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class DockerTest2 extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with NoLanguage {

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  def urlFromUriBase(uri: String): String = s"http://127.0.0.1:9001/$uri"
  def urlFromUriSocial(uri: String): String = s"http://127.0.0.1:9002/$uri"

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readSkilledPersons(), SkilledPersonFactory)
  }

  "Scala dude" should "should be found" in {
    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))
    CommonSearcher.searchAsync(searchedSkilledPerson, SkilledPersonFactory).map(result => {
      println(result)
      assert(result.length == 1)
      assert(result.head.obj.firstName.get == "Roger")
    })
  }

  "Cascaded search" should "find at least one set" in {

    case class PersonWithSocials(id: Int, personFound: SearchResult[Int, Person], socialsFound: Seq[SearchResult[Int, SocialPerson]])
      extends PkDataSet[Int](id)

    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))

    def combineFacebookScored(basePerson: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = {
      val restResponse: Future[SocialPersonColScored] =
        RestClient.post[Person, SocialPersonColScored](urlFromUriSocial("findSocialForScored"), basePerson.obj)
      println(basePerson.obj)
      restResponse.andThen {
        case Success(res) =>
          println("Seems we got it here")
          println(res)
        case Failure(t) => println(t.getMessage)
      }
      println("Is it returning???")
      restResponse.map(res => res.socialPersons)
    }

//    def combineBaseData(skilledPerson: SearchResult[Int, SkilledPerson]): Future[Seq[SearchResult[Int, Person]]] = {
//      val searchedBasePerson: Person = Person(-1, "", skilledPerson.obj.firstName.getOrElse("-"),
//        skilledPerson.obj.lastName.getOrElse("-"), "", "")
//      val restResponse: Future[PersonCollection] =
//        RestClient.post[Person, PersonCollection](urlFromUriBase("findBaseDataFor"), searchedBasePerson)
//      restResponse.map(res => res.persons)
//    }

    // This one should be moved to API once it is working. Not working at the moment. Very pissed...
    def combineBaseAndSocialData(skilledPerson: SearchResult[Int, SkilledPerson]): Future[Seq[SearchResult[Int, PersonWithSocials]]] = {
      val searchedBasePerson: Person = Person(-1, "", skilledPerson.obj.firstName.getOrElse("-"),
        skilledPerson.obj.lastName.getOrElse("-"), "", "")
      val restResponse: Future[PersonCollection] =
        RestClient.post[Person, PersonCollection](urlFromUriBase("findBaseDataFor"), searchedBasePerson)
      restResponse.map((res: PersonCollection) => {
        val fResult: Seq[SearchResult[Int, PersonWithSocials]] = for {
          person: SearchResult[Int, Person] <- res.persons
          // Tmp. Seems to work somehow, but in the end it is not working. Completely confusing.
          social:Seq[SearchResult[Int, SocialPerson]] = Await.result(combineFacebookScored(person), 5.seconds)
        } yield SearchResult[Int, PersonWithSocials](PersonWithSocials(-1, person, social), 0F)
        fResult
      })
    }


    MappingSearcher.searchCombined(input = searchedSkilledPerson, factory = SkilledPersonFactory,
      combineFn = combineBaseAndSocialData, secondLevelTimeout = 15.seconds).map(result => {
      println("GOT IT? PROBABLY NOT. F... IT.")
      println(result)
      assert(result.length == 1)
    })
  }
}
