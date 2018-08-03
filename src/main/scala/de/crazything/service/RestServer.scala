package de.crazything.service

//import javax.inject.Inject
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc._
import play.api.routing.Router
import play.api.{BuiltInComponents, NoHttpFiltersComponents}
import play.core.server._

import scala.concurrent.Future

object RestServer {

  def run(_serverConfig: ServerConfig, _router: Router): NettyServer = {
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
