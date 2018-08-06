package de.crazything.app.test

import de.crazything.app.Person._
import de.crazything.app.{NettyRunner, Person}
import de.crazything.service.{QuickJsonParser, RestClient}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll}
import play.core.server.NettyServer

class RestTest extends AsyncFlatSpec with BeforeAndAfterAll /*with FilterAsync*/ with QuickJsonParser {

  val server: NettyServer = NettyRunner.runServer
  val port: Int = server.httpPort.get

  override def afterAll: Unit = NettyRunner.stopServer()

  val standardPerson = Person(1, "Herr", "firstName", "lastName", "street", "city")

  def urlFromUri(uri: String): String = s"http://127.0.0.1:$port/$uri"

  "GET" should "at least work" in {
    RestClient.get[Person](urlFromUri("foo")).map(response => {
      assert(response == standardPerson)
    })
  }

  it should "find nothing for dummy uri" in {
    recoverToSucceededIf[RuntimeException] {
      RestClient.get[Person](urlFromUri("youNeverFindThis")).map(response => {
        assert(response == standardPerson)
      })
    }
  }

  "POST" should "receive an echo as case class" in {
    RestClient.post[Person, Person](urlFromUri("qux"), standardPerson).map(response => {
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
