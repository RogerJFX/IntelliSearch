MicroSearch
-

[![Build Status](https://travis-ci.com/RogerJFX/MicroSearch.svg?branch=master)](https://travis-ci.com/RogerJFX/MicroSearch)
[![Coverage Status](https://codecov.io/gh/rogerjfx/microsearch/branch/master/graph/badge.svg)](https://codecov.io/gh/RogerJFX/MicroSearch)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Another attempt to make search results most reliable.**

History of naming of this project is:

- Phonetic Search
- Reliable Search
- Micro Search
- IntelliSearch

There is a particular hierarchy here. 

- Phonetic makes simple searches more reliable in most cases. 
- A reliable search only should be reliable after passing some search processes. Filtering, mapping, whatever.
- Making search processes micro services has its own particular charm. So we can divide Lucene indices up and 
combine or filter results in the end.
- We added some very simple machine learning processes. So we decided to call it IntelliSearch

We decided to rename our project to MicroSearch, because we strongly believe even search indices might 
go to micro services.

There are many use cases for a reliable search:

1. in any case a reliable search improves results' quality
1. a reliable search might be used in scenarios, that are business critical (e.g. local database merging)
1. a real reliable search might gather and compare data from multiple services to 
    1. filter primary search results
    1. enhance primary search result by further matching search results
    1. in the end it might by used to obtain even more but still weighted, thus reliable results
    since it is possible to weight results after obtaining a tree of one to many results.

What we have so far
-

1. There is a sophisticated search procedure for any search (service) instance including optional
phonetic search. 
1. There are services based on Rest. Any searching service can
    1. search his own index an call another service for filtering issues.
    1. search his own index an call another service for mapping issues.
    1. search another service and call right another one for filtering or mapping.
    So this service does not need to have an own searching index, since it is just delegating.
1. We can filter over many services.
1. We can have real cascading stacks as well. 

A typical mapping result over three instances might look like (numbers are scores just for reliability)

*Some or None are Options in Scala. Sorry for your inconvenience*

*MappedResults and SearchResult are our own classes in "main", e.g. SkilledPerson is a test class*
~~~
List(
  MappedResults(
    SearchResult(
      SkilledPerson(21,Some("Roger"),Some("Hösl"),Some(List("Scala", "Java", "Lucene"))),27.38844),
      List(
        SearchResult(
          MappedResults(
            SearchResult(
              Person(2,"Herr","Roger","Hösl","Some street 25", "Some city"),49.444176),
              List(
                SearchResult(SocialPerson(2,"Roger","Hösl",Some("roger"),None),36.71957), 
                SearchResult(SocialPerson(14,"Roger","Hoesl",Some("roger"),Some("hoesl")),4.0875263)
              )
            ), 
        49.444176
      )
    )
  )
)
~~~

In this particular scenario we searched for persons with some skills in the first instance. 
After that we try to gather base data from a remote service. Upon those results we try to get some social media 
data from a further service.

Using filters instead of mappings of course would give us only one result list instead of getting this bunch of data
as a result.

The unit testing is fine so far, but of course we decided to run some integration tests.

**Currently there are 2 docker images**, that we put to their own sbt modules. A first test is working.
However only one docker instance is used so far. More coming soon. 
We plan to play around with them some time. And we sure will come to many docker images once we will start 
using Akka clusters.

To start the new docker images, run 

~~~
sbt clean dockerize
~~~

This will run the integration tests right after the dockered services are alive.

If you just want the docker containers keep alive, but want to run new tests towards them, call

~~~
sbt intTest
~~~

Attention! sbt test will fail, since the docker images will not be running then.

~~~
// FAILS! Call `sbt unitTest` OR `sbt intTest`
sbt test
~~~

Only unit test will be executed by

~~~
sbt unitTest
~~~

To come back to the first level search, which of course should be as exact as possible:
-

Any service instance of course does, what is described next.

There is a default hierarchy of Queries:

1. TermQuery => Perfect match is the winner in any case
1. WildcardQuery => Some suffix missing? Ok then
1. RegexQuery => Custom regular expressions might be inferred
1. PhoneticQuery => Some codecs, available for e.g. German or English
1. FuzzyQuery => Levenstein algorithm, just calculating distances.

In the end a user might write something like

~~~
  def createQuery(person: Person): Query = {
    Seq(
      (LAST_NAME, person.lastName).exact,
      (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex,
      (LAST_NAME, person.lastName, Boost.PHONETIC).phonetic,

      (FIRST_NAME, person.firstName, Boost.EXACT / 1.2F).exact,
      (FIRST_NAME, createWildCardTerm(person.firstName), Boost.WILDCARD / 1.5F).wildcard,
      (FIRST_NAME, createRegexTerm(person.firstName), Boost.REGEX / 2F).regex,
      (FIRST_NAME, person.firstName, Boost.PHONETIC / 2F).phonetic
    )
  }
~~~

Or even (which, agreed, in this particular example is not that reasonable)

~~~
  def createQuery(person: Person): Query = {
    Seq(
      Seq(
        (LAST_NAME, person.lastName).exact,
        (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex
      ).must,
      Seq(
        (FIRST_NAME, person.firstName).exact.must,
        (FIRST_NAME, createRegexTerm(person.firstName), Boost.EXACT * 2).regex.should
      ).must
    )
  }
~~~

More coming soon. 