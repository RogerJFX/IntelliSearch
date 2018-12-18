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

  def run(pServerConfig: ServerConfig, pRouter: Router): NettyServer = {
    logger.info("Running Embedded server in Mode {}. " +
        "For running in production mode consider setting up some module using the play framework.",
      pServerConfig.mode.toString)

    val components: NettyServerComponents with BuiltInComponents with NoHttpFiltersComponents =
      new NettyServerComponents with BuiltInComponents  with NoHttpFiltersComponents {
        override lazy val serverConfig: ServerConfig = pServerConfig
        override lazy val router: Router = pRouter
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
