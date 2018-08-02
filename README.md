ReliableSearch
-

[![Build Status](https://travis-ci.com/RogerJFX/ReliableSearch.svg?branch=master)](https://travis-ci.com/RogerJFX/ReliableSearch)
[![Coverage Status](https://codecov.io/gh/rogerjfx/reliablesearch/branch/master/graph/badge.svg)](https://codecov.io/gh/RogerJFX/ReliableSearch)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


**Another attempt to make search results most reliable.**

There are many use cases for a reliable search:

1. in any case a reliable search improves results' quality
1. a reliable search might be used in scenarios, that are business critical

Our strategy is to accumulate queries and the weighted results as well. That's for the first search.

**Another issue:** we might want to use some other services to check/filter our initial search's result. So we decided to 
implement some further methods taking a filter method as an argument. Filters even might be Futures. So it is possible 
to gain further information from remote services in order to make our initial result more precise. 
We think of soon implementing some test cases using Akka.

In some way we have a cascaded search then.

If we in the next step don't let the filters return booleans but filtered results, ... 
Yes, we should think over that. Currently the project is old less than 2 weeks.

####To come back to the initial search, which of course should be exact as possible:

There is a default hierarchy of Queries:

1. TermQuery => Perfect match is the winner in any case
1. WildcardQuery => Some suffix missing? Ok then
1. RegexQuery => Custom regular expressions might be inferred
1. PhoneticQuery => Some codecs, available for e.g. German or English
1. FuzzyQuery => Levenstein algorithm, just calculating distances.

In the end a user might write something like

~~~
  def createFirstAndLastNameQuery(person: Person): Query = {
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

... and combine it as he likes within his factory.

Our goal is to make searches not only reliable but most configurable - in the code, not in JSON. 
We strongly believe, reliability can only be achieved by a user who knows his case and is capable 
of doing modifications writing code.

The application is written in Scala, version 2.12.6 using Lucene version is 7.4.0.

One last note: I initially wrote Scala code, that would have been easily
translated into Java. I lost this scope and I am sorry for this. However it still 
is possible, even if it meanwhile would be a real pain in my holy *beep*.

####Some thoughts

- Making a service of it is a huge idea, though not in scope at the moment. But...
- Why not creating a REST interface?
- Why not using Akka? Even a cluster would appear reasonable.
- And why the hell shouldn't we think of passing initial factories or filters using the good old 
    Java serializer? Just combine REST with RMI. Why not? At least its worth a thought. Using 
    Akka may ease things. 
