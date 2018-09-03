//package de.crazything.app.test
//
//import de.crazything.app.Person._
//import de.crazything.app._
//import de.crazything.app.test.helpers.DataProvider
//import de.crazything.search.entity.{MappedResults, MappedResultsCollection, SearchResult}
//import de.crazything.search.ext.MappingSearcher
//import de.crazything.search.{CommonIndexer, DirectoryContainer}
//import de.crazything.service.{QuickJsonParser, RestClient}
//import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
//import org.slf4j.{Logger, LoggerFactory}
//import play.core.server.NettyServer
//
//import scala.concurrent.Future
//import scala.concurrent.duration._
//
//class RestCombineBigTest extends AsyncFlatSpec with BeforeAndAfterAll with QuickJsonParser with GermanLanguage with DirectoryContainer with FilterAsync{
//
//  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.RestCombineBigTest")
//
//  val server: NettyServer = NettyRunner.runServer
//  val port: Int = server.httpPort.get
//
//  override def beforeAll(): Unit = {
//    CommonIndexer.index(DataProvider.readVerySimplePersonsBig(), PersonFactoryDE)
//    CommonIndexer.index(DataProvider.readSocialPersonsBig(), SocialPersonFactory, "remoteIndex")
//    CommonIndexer.index(DataProvider.readSkilledPersonsBig(), SkilledPersonFactory, "skilledIndex")
//  }
//
//  override def afterAll: Unit = NettyRunner.stopServer()
//
//  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"
//
//  "Mapping" should "run locally" in {
//    val searchedSkilledPerson = SkilledPerson(-1, None, None, Some(Seq("Scala", "Postgresql")))
//
//    def combineBaseAndSocialData(skilledPerson: SearchResult[Int, SkilledPerson]):
//    Future[Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]]] = {
//      val searchedBasePerson: Person = Person(-1, "", skilledPerson.obj.firstName.getOrElse("-"),
//        skilledPerson.obj.lastName.getOrElse("-"), "", "")
//      val restResponse: Future[MappedResultsCollection[Int, Int, Person, SocialPerson]] =
//        RestClient.post[Person, MappedResultsCollection[Int, Int, Person, SocialPerson]](
//          urlFromUri("mapSocial2BaseBig"), searchedBasePerson)
//      val result: Future[Seq[SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]]]] =
//        restResponse.map(res => {
//          res.entries.map(rr => SearchResult[Int, MappedResults[Int, Int, Person, SocialPerson]](rr, rr.target.score))
//        })
//      result
//    }
//
//
//    MappingSearcher.search(input = searchedSkilledPerson, factory = SkilledPersonFactory,
//      searcherOption = "skilledIndex",
//      mapperFn = combineBaseAndSocialData,
//      secondLevelTimeout = 4.minutes,
//      maxHits = 10)
//      .map((result: Seq[MappedResults[Int, Int, SkilledPerson, MappedResults[Int, Int, Person, SocialPerson]]]) => {
//        println(result)
//        assert(result.length == 1)
//      })
//  }
//
//
//}
