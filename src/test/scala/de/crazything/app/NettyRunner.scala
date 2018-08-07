package de.crazything.app

import java.io.File
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import de.crazything.search.CommonSearcher
import de.crazything.search.entity.SearchResult
import de.crazything.service.{QuickJsonParser, RestServer}
import play.api.Mode
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.routing.sird._
import play.core.server.{NettyServer, ServerConfig}

object NettyRunner extends QuickJsonParser{

  val serverRef: AtomicReference[NettyServer] = new AtomicReference[NettyServer]()
  val testRunCalls: AtomicInteger = new AtomicInteger()

  val serverConfig = ServerConfig(
    this.getClass.getClassLoader,
    new File("."),
    Some(0),
    None,
    "127.0.0.1",
    Mode.Prod,
    System.getProperties)

  val router: Router = Router.from { // No, Router.from is not deprecated, but Tags above "from".
    case GET(p"/foo") => Action {
      Results.Created("""{"id":1,"salutation":"Herr","firstName":"firstName","lastName":"lastName","street":"street","city":"city"}""")
        .as("application/json")
    }
    case POST(p"/qux") => Action {
      request => {
        val body: JsValue = request.body.asJson.get
        Results.Created(body).as("application/json")
      }
    }
    case POST(p"/findSocialFor") => Action {
      request => {
        val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
        val socialPerson: SocialPerson = SocialPerson(-1, person.firstName, person.lastName)
        val searchResult: Seq[SearchResult[Int, SocialPerson]] = CommonSearcher.search(socialPerson, SocialPersonFactory)
        val strSearchResult: String = t2JsonString[SocialPersonCollection](SocialPersonCollection(searchResult.map(r => r.obj)))
        Results.Created(strSearchResult).as("application/json")
      }
    }
    case POST(p"/findSocialForScored") => Action {
      request => {
        val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
        val socialPerson: SocialPerson = SocialPerson(-1, person.firstName, person.lastName)
        val searchResult: Seq[SearchResult[Int, SocialPerson]] = CommonSearcher.search(socialPerson, SocialPersonFactory)
        val strSearchResult: String = t2JsonString[SocialPersonColScored](SocialPersonColScored(searchResult))
        Results.Created(strSearchResult).as("application/json")
      }
    }
    case _ => Action {
      Results.NotFound
    }
  }

  def runServer: NettyServer = {
    this.synchronized {
      if (testRunCalls.getAndIncrement() == 0) {
        val runningServer = RestServer.run(serverConfig, router)
        serverRef.set(runningServer)
        runningServer
      } else {
        serverRef.get()
      }
    }
  }

  def stopServer(): Unit = {
    this.synchronized {
      if (testRunCalls.decrementAndGet() == 0) {
        serverRef.get().stop()
      }
    }
  }
}
