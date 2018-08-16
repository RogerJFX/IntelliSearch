package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
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
