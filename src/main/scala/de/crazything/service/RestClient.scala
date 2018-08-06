package de.crazything.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.json.OFormat
import play.api.libs.ws.ahc.{AhcWSClientConfig, AhcWSClientConfigFactory, StandaloneAhcWSClient}
import play.api.libs.ws.{BodyWritable, InMemoryBody, StandaloneWSClient}

import scala.concurrent.{ExecutionContext, Future}

object RestClient extends QuickJsonParser{

  private val classLoader: ClassLoader = RestClient.getClass.getClassLoader

  private val config: Config = ConfigFactory.load(classLoader, "application.conf")

  private val ahcWsConfig: AhcWSClientConfig = AhcWSClientConfigFactory.forConfig(config)

  implicit val system: ActorSystem = ActorSystem("RestClient", config, classLoader)

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val String2Writable: BodyWritable[String] =
    BodyWritable(str => InMemoryBody(ByteString.fromString(str)), "application/json")


  def get[P](url: String)(implicit resFormat: OFormat[P], exc: ExecutionContext): Future[P] = {
    val wsClient: StandaloneWSClient = StandaloneAhcWSClient(ahcWsConfig)
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

  def post[T, P](url: String, payload: T)(implicit reqFormat: OFormat[T], resFormat: OFormat[P],
                                          exc: ExecutionContext): Future[P] = {
    val wsClient: StandaloneWSClient = StandaloneAhcWSClient(ahcWsConfig)
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
