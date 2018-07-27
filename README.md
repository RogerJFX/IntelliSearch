ReliableSearch
-

An attempt to make search results most reliable. 

There are many use cases for a reliable search:

1. in any case a reliable search improves result's quality
1. a reliable search might be used in scenarios, that are business critical

In the second case there is a strong need of tweaking from start/code on. So no need for any elastic stuff.

Think of the following scenario: we have two or more customer databases. Now we want to merge them into one database. 
In this case results must be reliable. Modifications should be as near as possible to the code then.

Our strategy is to accumulate queries and the weighted results as well.

Our goal is to make searches not only reliable but most configurable - in the code, not in JSON. We strongly believe, 
reliability can only be achieved by a user who knows his case and is capable of doing modifications writing code.

The application is written in Scala, version 2.12.6 using Lucene version is 7.4.0.

One last note: I initially wrote Scala code, that would have been easily
translated into Java. I lost this scope and I am sorry for this. However it still 
is possible, even if it meanwhile would be a real pain in my holy *beep*.
