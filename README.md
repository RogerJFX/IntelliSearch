ReliableSearch
-

Another attempt to make search results most reliable. 

There are many use cases for a reliable search:

1. in any case a reliable search improves results' quality
1. a reliable search might be used in scenarios, that are business critical

In the second case there is a strong need of tweaking from start/code on. So no need for any 
elastic stuff.

Think of the following scenario: we have two or more customer databases. Now we want to merge 
them into one database. In this case results must be reliable. Modifications should be as 
near as possible to the code then.

Our strategy is to accumulate queries and the weighted results as well.

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

Some thoughts
--

- Making a service of it is a huge idea, though not in scope at the moment. But...
- Why not creating a REST interface?
- Why not using Akka? Even a cluster would appear reasonable.
- And why the hell shouldn't we think of passing initial factories or filters using the good old 
    Java serializer? Just combine REST with RMI. Why not? At least its worth a thought. Using 
    Akka may ease things. 
