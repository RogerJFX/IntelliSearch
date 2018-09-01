package de.crazything.app

import java.io.File

import de.crazything.service.EmbeddedRestServer
import play.api.Mode
import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.routing.sird._
import play.core.server.ServerConfig

object Main extends App {

  if (args.length == 0) {
    throw new IllegalArgumentException("Please gimme a port")
  }

  val serverConfig = ServerConfig(
    this.getClass.getClassLoader,
    new File("."),
    Some(args(0).toInt),
    None,
    "0.0.0.0",
    Mode.Prod,
    System.getProperties)
  val router: Router = Router.from { // No, Router.from is not deprecated, but Tags above "from".
    case GET(p"/test") => LittleDataController.test
    // Little data testing
    case POST(p"/findBaseDataFor") => LittleDataController.findBaseData
    case POST(p"/findBaseDataForWithSocial") => LittleDataController.findBaseDataForWithSocial
    case POST(p"/mapSocial2Base") => LittleDataController.mapSocial2Base
    case _ => Action {
      Results.NotFound
    }
  }

  EmbeddedRestServer.run(serverConfig, router)

}
