package de.crazything.app.test

import java.io.File

import akka.actor.ActorSystem
import play.api.routing.Router
import akka.stream.ActorMaterializer
import akka.util.ByteString
import de.crazything.app.Person._
import de.crazything.app.Person
import de.crazything.service.{QuickJsonParser, RestClient, RestServer}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
import play.api.Mode
import play.api.libs.json.{JsValue, OFormat}
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.ws.{BodyWritable, InMemoryBody, StandaloneWSClient}
import play.api.mvc.Action
import play.core.server.NettyServer
import play.api.routing.sird._
import play.core.server._
import play.api.mvc._

import scala.concurrent.Future

class RestTest extends AsyncFlatSpec with BeforeAndAfterAll with FilterAsync with QuickJsonParser {



  private val mimeType = "application/json"

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
    case _ => Action {
      Results.NotFound
    }
  }

  val server: NettyServer = RestServer.run(serverConfig, router)
  val port: Int = server.httpPort.get

  override def afterAll: Unit = server.stop()

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

//  def get[P](uri: String)(implicit resFormat: OFormat[P]): Future[P] = {
//    val wsClient: StandaloneWSClient = StandaloneAhcWSClient()
//    val futureResponse: Future[P] =
//      wsClient.url(urlFromUri(uri))
//        .get()
//        .map(wsResponse => {
//          if (wsResponse.status < 200 || wsResponse.status > 201) {
//            throw new RuntimeException(s"We got a HTTP status of ${wsResponse.status}. This does not seem to be ok.")
//          }
//          jsonString2T[P](wsResponse.body)
//        })
//    futureResponse.andThen {
//      case _ => wsClient.close()
//    }
//  }
//
//  def post[T, P](uri: String, payload: T)(implicit reqFormat: OFormat[T], resFormat: OFormat[P]): Future[P] = {
//    implicit val String2Writable: BodyWritable[String] = {
//      BodyWritable(str => InMemoryBody(ByteString.fromString(str)), mimeType)
//    }
//    val wsClient: StandaloneWSClient = StandaloneAhcWSClient()
//    val futureResponse: Future[P] =
//      wsClient.url(urlFromUri(uri))
//        .post(t2JsonString[T](payload))
//        .map(wsResponse => {
//          if (wsResponse.status < 200 || wsResponse.status > 201) {
//            throw new RuntimeException(s"We got a HTTP status of ${wsResponse.status}. This does not seem to be ok.")
//          }
//          jsonString2T[P](wsResponse.body)
//        })
//    futureResponse.andThen {
//      case _ => wsClient.close()
//    }
//  }

  "GET" should "at least work" in {
    val standardPerson = Person(1, "Herr", "firstName", "lastName", "street", "city")
    RestClient.get(urlFromUri("foo")).map(response => {
      assert(response == standardPerson)
    })
  }

  it should "find nothing for dummy uri" in {
    recoverToSucceededIf[RuntimeException] {
      RestClient.get(urlFromUri("youNeverFindThis")).map(response => {
        assert(response == standardPerson)
      })
    }
  }

  "POST" should "receive an echo as case class" in {
    val standardPerson = Person(1, "Herr", "firstName", "lastName", "street", "city")
    RestClient.post(urlFromUri("qux"), standardPerson).map(response => {
      assert(response == standardPerson)
    })
  }

  it should "throw some not found error" in {
    recoverToSucceededIf[RuntimeException] {
      RestClient.post(urlFromUri("qux12121212"), standardPerson).map(response => {
        assert(response == standardPerson)
      })
    }
  }

}
