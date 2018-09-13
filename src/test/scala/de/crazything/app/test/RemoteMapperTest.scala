package de.crazything.app.test

import de.crazything.app._
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.{SearchResult, SearchResultCollection}
import de.crazything.search.ext.MappingSearcher
import de.crazything.search.{CommonIndexer, DirectoryContainer}
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
//import org.slf4j.{Logger, LoggerFactory}
import play.core.server.NettyServer

import scala.concurrent.Future

class RemoteMapperTest extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with GermanLanguage with DirectoryContainer with FilterAsync{

  //private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.RemoteMapperTest")

  val server: NettyServer = NettyRunner.runServer
  val port: Int = server.httpPort.get

  override def beforeAll(): Unit = {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
    CommonIndexer.index(DataProvider.readSocialPersons(), SocialPersonFactory, "remoteIndex")
    CommonIndexer.index(DataProvider.readSkilledPersons(), SkilledPersonFactory, "skilledIndex")
  }

  override def afterAll: Unit = NettyRunner.stopServer()

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

  def combineFacebookScored(skilledPerson: SearchResult[Int, SkilledPerson]): Future[Seq[SearchResult[Int, SocialPerson]]] = {
    val searchedBasePerson: Person = Person(-1, "", skilledPerson.found.firstName.getOrElse("-"),
      skilledPerson.found.lastName.getOrElse("-"), "", "")
    val restResponse =
      RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUri("findSocialForScored"),
        searchedBasePerson)
    restResponse.map(res => res.entries)
  }

  "Mapping" should "work completely remote" in {
    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))



    MappingSearcher.searchRemote(input = searchedSkilledPerson, url = urlFromUri("findSkilledPerson"),
      mapperFn = combineFacebookScored).map(result => {
      println(result)
      assert(result.length == 1)
    })
  }

  it should "work passing a function" in {
    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))

    def getInitialData: Future[Seq[SearchResult[Int, SkilledPerson]]] = {
      val restResponse: Future[SearchResultCollection[Int, SkilledPerson]] =
        RestClient.post[SkilledPerson, SearchResultCollection[Int, SkilledPerson]](urlFromUri("findSkilledPerson"),
          searchedSkilledPerson)
      restResponse.map(res => res.entries)
    }

    MappingSearcher.searchFuture(initialFuture = getInitialData, mapperFn = combineFacebookScored).map(result => {
      println(result)
      assert(result.length == 1)
    })
  }
}
