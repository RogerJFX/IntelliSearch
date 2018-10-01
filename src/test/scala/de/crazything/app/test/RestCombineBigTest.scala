package de.crazything.app.test

import de.crazything.app.entity.Person._
import de.crazything.app._
import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.{Person, SkilledPerson, SocialPerson}
import de.crazything.app.factory.{PersonFactoryDE, SkilledPersonFactory, SocialPersonFactory}
import de.crazything.app.helpers.DataProvider
import de.crazything.search.entity.{MappedResults, MappedResultsCollection, SearchResult}
import de.crazything.search.ext.MappingSearcher
import de.crazything.search.{CommonIndexer, DirectoryContainer}
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
import org.slf4j.{Logger, LoggerFactory}
import play.core.server.NettyServer

import scala.concurrent.Future
import scala.concurrent.duration._

class RestCombineBigTest extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with GermanLanguage
  with DirectoryContainer {

  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.RestCombineBigTest")

  val server: NettyServer = NettyRunner.runServer
  val port: Int = server.httpPort.get

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readVerySimplePersonsBig(), PersonFactoryDE)
    CommonIndexer.index(DataProvider.readSocialPersonsBig(), SocialPersonFactory, "remoteIndex")
    CommonIndexer.index(DataProvider.readSkilledPersonsBig(), SkilledPersonFactory, "skilledIndex")
  }

  override def afterAll: Unit = NettyRunner.stopServer()

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

  // Ignored. Only tested in single mode.
  "Mapping" should "run locally" ignore {
    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Ecmascript", "Postgres", "Scala", "Linux", "Java")))

    def combineBaseAndSocialData(skilledPerson: SearchResult[Int, SkilledPerson]):
    Future[Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]]] = {
      val searchedBasePerson: Person = Person(-1, "", skilledPerson.found.firstName.getOrElse("-"),
        skilledPerson.found.lastName.getOrElse("-"), "", "")
        RestClient.post[Person, MappedResultsCollection[Int, Int, Person, SocialPerson]](
          urlFromUri("mapSocial2BaseBig"), searchedBasePerson)
    }

    MappingSearcher.search(input = searchedSkilledPerson, factory = SkilledPersonFactory,
      searcherOption = "skilledIndex",
      mapperFn = combineBaseAndSocialData,
      secondLevelTimeout = 4.minutes,
      maxHits = 2)
      .map((result: Seq[MappedResults[Int, Int, SkilledPerson, MappedResults[Int, Int, Person, SocialPerson]]]) => {
        val firstSkilledPerson: SkilledPerson = result.head.target.found
        val firstHitMappings: Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]] = result.head.results
        val firstPerson: Person = firstHitMappings.head.found.target.found
        val firstPersonSocialHits: Seq[SearchResult[Int, SocialPerson]] = firstHitMappings.head.found.results
        val firstSocialPerson: SocialPerson = firstPersonSocialHits.head.found
        result.foreach(sp => println(sp.target))
        result.head.results.foreach(sp => println(sp.found.target))
        println(s"Found skilled person is: $firstSkilledPerson")
        println(s"Found base person is: $firstPerson")


        println(s"Found social person is: $firstSocialPerson")

        assert(firstSkilledPerson.firstName.get == "Burchard")
        assert(firstSkilledPerson.lastName.get == "Stoeckl")
        assert(firstPerson.firstName == "Burkhard")
        assert(firstPerson.lastName == "Stöckl")
        assert(firstPersonSocialHits.length == 100)
        assert(firstSocialPerson.firstName == "Burchard")
        assert(firstSocialPerson.lastName == "Stoeckl")
        assert(result.length == 2)
      })
  }


}
