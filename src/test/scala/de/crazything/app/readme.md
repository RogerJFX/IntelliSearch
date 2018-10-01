Manifesto
-
These tests here might seem a bit strange to some people out there.

Questions might be:

1. Where are the unit tests?
1. Why the hell isn't there any mock?
1. Why is there something called integration test?
1. Why does the author still renounce the godsend dependency injection?
1. 100% coverage? This must be cheating.

Ok, for a start, this is a really tiny application so far. So it 
still is possible to test honestly without any pain. No need for cheats. 
Come on! You never saw mocks that simply are cheating, 
thus completely pointless? Just kidding the coverage?

**And the application should stay tiny.** It simply is a core implementation, 
that tries to get the point. So the best way testing is having some representative 
application elaborating any point of the implementation.

The benefits are:

1. This project **really is** test driven.
1. Tests will not become messy that fast, since they live in a way.
1. Option of saving tons of redundant and thus pointless tests.
1. Option of reusing tests in e.g. test scenarios or integration tests.
1. Forcing developers to write real clean code even without DI or other crap.
1. Making mocks as known obsolete.

Ok, we know this is easy talking. We don't have to deal with events, observers or 
anything like those. As we already mentioned it is a tiny application.

Let's manifest we still follow the above stated rules. We even do not mock any 
RESTful request, but test having **real** REST clients talking to a 
**real** service. No, not what you might think: **real** only in test scope.

Convinced from tests only being meaningful if as honest possible, we should call our 
strategy **"Honest Testing"**. 

That's for the manifesto.
