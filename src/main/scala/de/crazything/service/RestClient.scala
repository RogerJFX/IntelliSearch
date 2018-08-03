package de.crazything.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import play.api.libs.json.OFormat
import play.api.libs.ws.{BodyWritable, InMemoryBody, StandaloneWSClient}
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RestClient extends QuickJsonParser{

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val mimeType = "application/json"

  def get[P](url: String)(implicit resFormat: OFormat[P]): Future[P] = {
    val wsClient: StandaloneWSClient = StandaloneAhcWSClient()
    val futureResponse: Future[P] =
      wsClient.url(url)
        .get()
        .map(wsResponse => {
          if (wsResponse.status < 200 || wsResponse.status > 201) {
            throw new RuntimeException(s"We got a HTTP status of ${wsResponse.status}. This does not seem to be ok.")
          }
          jsonString2T[P](wsResponse.body)
        })
    futureResponse.andThen {
      case _ => wsClient.close()
    }
  }

  def post[T, P](url: String, payload: T)(implicit reqFormat: OFormat[T], resFormat: OFormat[P]): Future[P] = {
    implicit val String2Writable: BodyWritable[String] = {
      BodyWritable(str => InMemoryBody(ByteString.fromString(str)), mimeType)
    }
    val wsClient: StandaloneWSClient = StandaloneAhcWSClient()
    val futureResponse: Future[P] =
      wsClient.url(url)
        .post(t2JsonString[T](payload))
        .map(wsResponse => {
          if (wsResponse.status < 200 || wsResponse.status > 201) {
            throw new RuntimeException(s"We got a HTTP status of ${wsResponse.status}. This does not seem to be ok.")
          }
          jsonString2T[P](wsResponse.body)
        })
    futureResponse.andThen {
      case _ => wsClient.close()
    }
  }
}
