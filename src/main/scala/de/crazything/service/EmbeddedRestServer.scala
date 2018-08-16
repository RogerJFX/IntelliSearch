package de.crazything.service

import org.slf4j.{Logger, LoggerFactory}
import play.api.Mode
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc._
import play.api.routing.Router
import play.api.{BuiltInComponents, NoHttpFiltersComponents}
import play.core.server._

import scala.concurrent.Future

object EmbeddedRestServer {

  private val logger: Logger = LoggerFactory.getLogger(EmbeddedRestServer.getClass)

  def run(_serverConfig: ServerConfig, _router: Router): NettyServer = {
    if(_serverConfig.mode == Mode.Prod) {
      logger.warn("This embedded server may not fit your needs in Production mode. " +
        "Consider setting up some module using the play framework.")
    }
    val components =
      new NettyServerComponents with BuiltInComponents  with NoHttpFiltersComponents {
        override lazy val serverConfig: ServerConfig = _serverConfig
        override lazy val router: Router = _router
        override lazy val httpErrorHandler: DefaultHttpErrorHandler = new DefaultHttpErrorHandler(environment,
          configuration, sourceMapper, Some(router)) {
          override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
            Future.successful(Results.NotFound("Nothing was found!"))
          }
        }
      }
    components.server
  }

}
