package de.crazything.app

import java.io.File

import de.crazything.app.NettyRunner.{jsonString2T, t2JsonString}
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.SearchResult
import de.crazything.search.{CommonIndexer, CommonSearcher}
import de.crazything.service.EmbeddedRestServer
import play.api.Mode
import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.routing.sird._
import play.core.server.ServerConfig

object Main extends App with GermanLanguage{

  if(args.length == 0) {
    throw new IllegalArgumentException("Please gimme a port")
  }

  CommonIndexer.index(DataProvider.readVerySimplePersonsResource(), PersonFactoryDE)

  val serverConfig = ServerConfig(
    this.getClass.getClassLoader,
    new File("."),
    //Some(0),
    Some(args(0).toInt),
    None,
    "0.0.0.0",
    Mode.Prod,
    System.getProperties)

  val router: Router = Router.from { // No, Router.from is not deprecated, but Tags above "from".
    case GET(p"/test") => Action {
      Results.Ok("It works! I got base data 4u.")
    }
    case POST(p"/findBaseDataFor") => Action {
      request => {
        val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
        val searchResult: Seq[SearchResult[Int, Person]] =
          CommonSearcher.search(input = person, factory = PersonFactoryDE)
        val strSearchResult: String = t2JsonString[PersonCollection](PersonCollection(searchResult))
        Results.Created(strSearchResult).as("application/json")
      }
    }
    case _ => Action {
      Results.NotFound
    }
  }

  val runningServer = EmbeddedRestServer.run(serverConfig, router)

}
