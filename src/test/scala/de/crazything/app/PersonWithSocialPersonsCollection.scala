package de.crazything.app

import de.crazything.search.entity.{PkDataSet, SearchResult}
import play.api.libs.json.{Json, OFormat}

case class PersonWithSocialResults(person: SearchResult[Int, Person], socialResults: Seq[SearchResult[Int, SocialPerson]])
  extends PkDataSet[Int](person.obj.id)

object PersonWithSocialResults {

  implicit def format: OFormat[PersonWithSocialResults] = Json.format[PersonWithSocialResults]

}

case class PersonWithSocialPersonsCollection(found: Seq[PersonWithSocialResults])

object PersonWithSocialPersonsCollection {

  implicit def format: OFormat[PersonWithSocialPersonsCollection] = Json.format[PersonWithSocialPersonsCollection]

}