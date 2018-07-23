PhoneticSearch
-

This project is in a very early state (started in 2018, 21th of July). 
Yet it should appear to be a proof of concept. It was a weekend, and I had some beer, sorry.

It is written in Scala, version 2.12.6. The topic is a search engine using Lucene. 
The Lucene version is 7.4.0.

The idea is to at least find matches based on phonetic conditions prior to fuzzy matches.

Remember this: language is first spoken, then written. So a phonetic search does make sense. 

Final goal is to deduce reliable data out of fuzzy data. 

This approach only focuses on the German language so far. 

###What's next:

- Getting rid of all those tmp shortcuts, that should lead to Exceptions in 
    any production. NullPointers, of course.
- Working out the API (not to be mentioned)
- Creating a service using Netty/Akka or even Play (no Solr, please)
- Creating an interface for final remote plausibility tests, if needed. 
    Maybe Kafka or Akka streams, or whatever.
- Extending the engine to other languages.
- Making it configurable. 


