package de.crazything.app

import java.io.File

import de.crazything.app.NettyRunner.{jsonString2T, t2JsonString}
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.{MappedResults, MappedResultsCollection, SearchResult, SearchResultCollection}
import de.crazything.search.ext.MappingSearcher
import de.crazything.search.{CommonIndexer, CommonSearcher}
import de.crazything.service.{EmbeddedRestServer, RestClient}
import play.api.Mode
import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.routing.sird._
import play.core.server.ServerConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
object Main extends App with GermanLanguage with Network{

  if(args.length == 0) {
    throw new IllegalArgumentException("Please gimme a port")
  }

  CommonIndexer.index(DataProvider.readVerySimplePersonsResource(), PersonFactoryDE)

  def combineFacebookScored(basePerson: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = {
    val restResponse: Future[SearchResultCollection[Int, SocialPerson]] =
      RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUriSocial("findSocialForScored"), basePerson.obj)
    restResponse.map(res => res.entries)
  }

  val serverConfig = ServerConfig(
    this.getClass.getClassLoader,
    new File("."),
    //Some(0),
    Some(args(0).toInt),
    None,
    "0.0.0.0",
    Mode.Prod,
    System.getProperties)
  val test = "test"
  val router: Router = Router.from { // No, Router.from is not deprecated, but Tags above "from".
    case GET(p"/$test") => Action {
      Results.Ok("It works! I got base data 4u.")
    }
    case POST(p"/findBaseDataFor") => Action {
      request => {
        val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
        val searchResult: Seq[SearchResult[Int, Person]] =
          CommonSearcher.search(input = person, factory = PersonFactoryDE)
        val strSearchResult: String = t2JsonString[SearchResultCollection[Int, Person]](SearchResultCollection(searchResult))
        Results.Created(strSearchResult).as("application/json")
      }
    }
    case POST(p"/findBaseDataForWithSocial") => Action.async {
      request => {
        val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
        MappingSearcher.searchMapping(input = person, factory = PersonFactoryDE,
          mapperFn = combineFacebookScored, secondLevelTimeout = 5.seconds).map((searchResult: Seq[MappedResults[Int, Int, Person, SocialPerson]]) => {
          val sequence: Seq[PersonWithSocialResults] = searchResult.map(sr => PersonWithSocialResults(sr.target, sr.results))
          val strSearchResult: String = t2JsonString[PersonWithSocialPersonsCollection](PersonWithSocialPersonsCollection(sequence))
          Results.Created(strSearchResult).as("application/json")
        })
      }
    }
    case POST(p"/mapSocial2Base") => Action.async {
      request => {
        val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
        MappingSearcher.searchMapping(input = person, factory = PersonFactoryDE,
          mapperFn = combineFacebookScored, secondLevelTimeout = 5.seconds).map(searchResult => {

          val strSearchResult: String = t2JsonString[MappedResultsCollection[Int, Int, Person, SocialPerson]](MappedResultsCollection(searchResult))
          Results.Created(strSearchResult).as("application/json")
        })
      }
    }
    case _ => Action {
      Results.NotFound
    }
  }

  val runningServer = EmbeddedRestServer.run(serverConfig, router)

}
