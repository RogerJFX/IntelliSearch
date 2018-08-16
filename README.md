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
that might be filtered later?

That's what we are about at this very moment. Looks pretty fine so far.

And yes, we should think about some website explaining and documenting all this. Stay tuned.

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
      (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex.must,
      (LAST_NAME, person.lastName, Boost.PHONETIC).phonetic.must,
      (LAST_NAME, "Trump").exact.mustNot
    )
  }
~~~

More coming soon. 