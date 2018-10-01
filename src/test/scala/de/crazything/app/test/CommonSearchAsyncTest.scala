package de.crazything.app.test

import de.crazything.app.factory.PersonFactoryDE
import de.crazything.app.helpers.DataProvider
import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.Person
import de.crazything.search.{CommonIndexer, CommonSearcher, QueryConfig}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfter}

class CommonSearchAsyncTest extends AsyncFlatSpec with BeforeAndAfter with QueryConfig with GermanLanguage{

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  before {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
  }

  "AsyncTest" should "do what we expect" in {
    CommonSearcher.searchAsync(input = standardPerson.copy(lastName = "Reißer"), factory = PersonFactoryDE).map(result => {
      assert(result.length == 1)
    })
  }
}
