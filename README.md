ReliableSearch
-

[![Build Status](https://travis-ci.com/RogerJFX/ReliableSearch.svg?branch=master)](https://travis-ci.com/RogerJFX/ReliableSearch)
[![Coverage Status](https://codecov.io/gh/rogerjfx/reliablesearch/branch/master/graph/badge.svg)](https://codecov.io/gh/RogerJFX/ReliableSearch)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


**Another attempt to make search results most reliable.**

There are many use cases for a reliable search:

1. in any case a reliable search improves results' quality
1. a reliable search might be used in scenarios, that are business critical (e.g. local database merging)

Our strategy is to accumulate queries and the weighted results as well. That's for the first level search.

We decided to introduce a second level searching process. Think of having another directory, database or whatever 
service giving us more information. And think of this information sources are somewhere remote. So why not filtering 
first level results by remote services' results? Why not mapping them in order to get a tree of more qualified results, 
that might be filtered later? Even unions would be reasonable.

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

Or even

~~~
  def createQuery(person: Person): Query = {
    Seq(
      (LAST_NAME, person.lastName).exact.should,
      (LAST_NAME, createRegexTerm(person.lastName)).regex.must,
      (LAST_NAME, person.lastName).phonetic.must,
      (LAST_NAME, "Trump").exact.mustNot
    )
  }
~~~

More coming soon. 