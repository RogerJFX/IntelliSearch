package de.crazything.app.test

import de.crazything.search.DirectoryContainer
import org.scalatest.AsyncFlatSpec

import scala.concurrent.Future

class DirectoryContainerTest extends AsyncFlatSpec {

  "Directory" should "not be found for alien name" in {
    recoverToSucceededIf[Exception](
      Future {
        DirectoryContainer.pickDirectoryForName("SOME VERY STRANGE NAME :P")
      }
    )
  }

}
