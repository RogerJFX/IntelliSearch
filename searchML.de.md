Die Problematik einer Suchmaschine
---
Autor: Roger F. Hösl

Datum: 27.12.2018

Version: 0.9.remarkable

---
1. Who the fuck is Roger?
1. Allgemeine Problematik
1. Das Problem der Ausgangs-Daten
1. Was eigentlich soll gefunden werden?
1. Kriterien für die Qualität einer Suche
1. Kriterien für die Qualität einer Relevanz-Sortierung
1. Das Lucene Scoring und Möglichkeiten der Einflussnahme
1. Machine Learning als Ansatz
1. Best Practices


---

**1. Who the fuck is Roger?**

Roger beschäftigt sich seit 2007 mit Lucene und darauf aufbauenden Frameworks wie Solr oder ElasticSearch. Er kann auf einige sehr erfolgreiche Projekte zurückblicken.

- 2007: BMW M-Power Community Side
- 2008: Milupa Community Side
- 2008 - 2010: Eigenes CMS für mobile Endgeräte mit integrierter Suche unter Lucene. Kunden z.B. Sparkasse, Süwag, Opel, Seat. Hier ohne Search Suggest wegen mobile.
- 2012: Implementierung einer neuen Suche für allyouneed.com (heute allyouneedfresh.com). Umsatz des Online-Supermarkts stieg nach Livegang der Suche sprunghaft um 30%. Allyouneedfresh hat seither viele Awards gewonnen.
- 2018: Start eines eigenen Search-Frameworks unter Scala, eben IntelliSearch.

Bis auf die Suche des Online-Supermarktes, die in Solr implementiert wurde, sind alle anderen in Lucene selbst verwirklicht. Sowohl Solr als auch ElasticSearch sind aber als nicht weiteres als Vereinfachungen bzw. Marketing-Coups zu verstehen. Unter der Haube steckt stets  Lucene.

---

**2. Allgemeine Problematik**

Eine Suche ist nie logisch. Ich kann also nicht aus bestimmten Sachverhalten auf weitere schließen. Sie ist stets unscharf, sodass man nur mit Hilfe von Mengen bzw. Schnittmengen die Qualität einer Suche beurteilen kann. Alleine der Versuch, die Qualität einer Suche logisch beurteilen zu wollen und daraus vielleicht gar Konsequenzen zu Verbesserungen herzuleiten, wird unweigerlich zur Verschlechterung derselben führen.

Drehe ich nämlich an irgendeinem Parameter, damit die Anwendung meiner logischen Anforderung gerecht wird, fliegen mir n andere Suchen um die Ohren. Es ist der berühmte Sack voller Flöhe.

Im Folgenden sollen Wege aufgezeigt werden.

---

**3: Das Problem der Ausgangs-Daten**

Es ist immer das Gleiche: die Suche funktioniert schon, dann will man etwas besser machen, und dann stellt man plötzlich fest, dass das Ausgangs-Material einfach wie Kraut und Rüben daherkommt. Ja, was will ich da denn machen? Rechtschreibfehler, komische Formatierungen im Text (Vorsicht Sonderzeichen oder German Umlauts), uneinheitliche Schreibweisen, Angaben in der falschen Datenbank-Spalte, usw..

*Shit in, shit out*

Nehmen wir einen Shop als Bespiel:

Datenpfleger A schreibt richtigerweise:

~~~
| Marke        | Produkt
|--------------|------------------------------
| Alete        | Karotte mit Hühnchen
~~~

Datenpfleger B schreibt:

~~~
| Marke        | Produkt
|--------------|------------------------------
|              | Alete Karotte mit Hühnchen
~~~

Datenpfleger C macht Copy&Paste, und dann steht da möglicherweise in unseren Daten (kein Scherz, gab es alles schon - hier würde ich sofort M$ Word und docx vermuten):

~~~
| Marke        | Produkt
|--------------|------------------------------
| Alete        | Karotte mit H&#252;hnchen
~~~

Was soll jetzt der arme Programmierer machen? Etwa alles mit einplanen? Alles??? Niemals!

Also rennt man zum Chef und teilt ihm mit, dass die Qualität einer Suche nun einmal steht und fällt mit der Qualität der zur Verfügung stehenden Daten. Dann gibt es Workshops usw..

Auf dem freien Markt sieht es wieder anders aus. Da gibt es SEO-Spezialisten, die aus ihrer Erfahrung heraus wissen, was man tun muss, um von Google gefunden zu werden. 

Schlecht:

~~~
Das Produkt Alete Karotte mit Hühnchen führen wir schon sehr lange.
~~~

Besser:

~~~
Das Produkt Karotte mit Hühnchen von Alete führen wir schon sehr lange.
~~~

Alleine das Wort "von" sagt in der Formalen Semantik einiges. Und Google ist auf jeden Fall soweit, solches zu interpretieren.

Es kann nur zum Schluss gekommen werden, dass die Veranlassung von besseren Ausgangs-Daten immer Sinn macht. Dafür kommen immer Leitfäden in Betracht, die von solchen, die bei einer Suche gefunden werden wollen, beherzigt werden sollten. Allzu offen sollte man aber nicht sein. Auf keinen Fall sollte man seine Algorithmen erklären, eben um sich nicht festnageln zu lassen. Lediglich zu einer gewissen Ordnung sollte aufgefordert werden, einer Ordnung eben, auf der man dann ein ordentliche Suche aufbauen kann.

---

**4. Was eigentlich soll gefunden werden?**

So viel wie möglich oder so wenig wie möglich?

Der Wissenschaftler Alexander S. wäre wahrscheinlich enttäuscht von einer Suche, die auf den Suchbegriff "rot" etwas weißes als Ergebnis liefert. Das selbst dann, wenn es etwas in rot nicht gibt. 

Sollte aber Frau Helene F. nach einem roten Unterröckchen suchen, das aber gerade ausverkauft ist, freut sie sich evtl. über ein angebotenes weißes, das ihr doch auch ganz gut stehen würde.

Also welche Konditionen? UND oder ODER?

Die Strategie UND filtert maximal (Herr S. wäre damit zufrieden). Es kommen also eher wenige Ergebnisse hoch.

Die Strategie ODER ist die Verkäufer-Strategie. Es soll so viel wie möglich gefunden werden, auch wenn die Ergebnisse sehr schnell unlogisch werden. Frau F. wird sich möglichweise für den weißen Unterrock entscheiden.

Und wie mache ich beide glücklich? Ganz einfach: ich sortiere richtig.

Es gibt noch eine dritte Abart: was, wenn ich etwas gezielt verkaufen will, also den Kunden tatsächlich manipulieren will?

- Marge höher?
- "Kunden, die das kauften, kauften auch"?

Dann spielt eine Business Logik ein, die allerdings bisher nicht entscheidende Fortschritte erzielen konnte. BI (Business Intelligence) als Begriff ist gegenwärtig nicht mehr besonders beliebt.

**Es bleibt bei der richtigen Sortierung.** Und dies bedeutet Stapelung von Queries, alle ODER-verknüpft, und alle unterschiedlich geboostet. U.U. sogar kaskadiert (dann kämen auch wieder UND-Verküpfungen ins Spiel).

Keine Bange um die Performance. Das ist Lucene ziemlich wurst, Lucene ist hier nachgerade unheimlich.

---

**5. Kriterien für die Qualität einer Suche**

Wir haben gesehen, dass man die Qualität einer Suche nicht logisch messen kann. Das Problem ist eben nicht formeller Natur wie etwa bei einer komplexen Gleichung, die aufzulösen ist. Darüberhinaus gibt es zu viele Abhängigkeiten und Interessen.

Eine einzelne Aussage zur Beurteilung einer Suche ist vulgo zu belächeln.

Eine Strategie zur Verbesserung einer Suche war bisher das sogenannte "Search Suggest". So unterstüzt man den Nutzer interaktiv, indem man ihm Vorschläge macht, wonach er eigentlich suchen könnte. Hier kommt es natürlich auf Performance an, denn der Nutzer sollte möglichst nach jeder Änderung seiner Eingabe durch Tastendruck eine neue Vorschlagsliste erhalten. Neben der spielerischen Komponente, die den Nutzer nach allen Erfahrungen länger bindet, erwies sich die Interaktion als wirkungsvolle Methode zur Filterung und Inspiration, sodass der Nutzer tatsächlich schneller fündig wird. Von der Zufriedenheit des Nutzers ganz zu schweigen.

Search Suggest verbessert eine Suche auf jeden Fall. Hier braucht man nicht zu messen. Eine gute Umsetzung eines solchen Search Suggest steigerte den Umsatz von allyouneed schlagartig um 30%. Man kann also doch messen.

Ja, man kann die Qualität einer Suche messen. Allerdings nur statistisch. Gemeinhin spricht man von der sogenannten Conversion Rate. Bei der eben genannten Suche ist das einfach: der Umsatz sagt fast alles aus. So würde eine Verschlechterung der Suche sich eher negativ auf den Umsatz auswirken.

Nehmen wir ein Beispiel von allyouneed: ich suche nach "Becks" (nicht "Beck's", da ist alles normal). Die ersten zwei Vorschläge sind die erwarteten Biere. Dann aber kommt ein gerade populärer Gin. Schlecht? Warum schlecht? Da will also jemand Alkoholika kaufen, vielleicht für eine Party, und dann braucht er doch wohl auch guten Gin. Kauft er nun tatsächlich den Gin, wirkt sich das sehr gut auf die Conversion Rate aus. Also was soll daran schlecht sein? Offenbar haben die aktuellen Entwickler von allyouneed populäre Produkte promoted, was zwar für unser Ermessen zu einer Verschlechterung führt, wohl aber auch zur Steigerung des Umsatzes - ergo ist es eine tatsächliche Verbesserung.

Aber nicht jeder hat es so einfach wie ein Onlineshop. 

---

**6. Kriterien für die Qualität einer Relevanz-Sortierung**

Hier sollte man das Nutzerverhalten aufzeichnen und auswerten. Eine optimale Conversion Rate sieht aus wie folgt:

Sei Y die Position eines Eintrags in der Ergebnisliste einer Suche
Sei N die Nummer des Klicks auf einen Eintrag nach einer Suche
Dann gilt für ein Optimum:

~~~
Y = N
~~~

In Worten: eine optimale Relevanz-Sortierung serviert dem Nutzer eine Liste von Ergebnis, die er der Reihe nach von oben bis unten anklickt. Schlecht wäre, wenn sich der Nutzer zuerst für das 30te Ergebnis interessiert.

Sei Q die Qualität, so gilt beispielsweise (auch andere Formeln oder Gewichtungen sind denkbar):

~~~
Q = - (Y - N)²
~~~

Beobachte ich nun das Nutzerverhalten und werte ich diese Daten aus, kann ich basierend auf der Zeit Tendenzen beobachten. Etwa nachdem ich ein wenig an den Stellschrauben der Suche drehte. Bei großen Datenmengen gewinne ich so einen objektiven Eindruck.

---

**7. Das Lucene Scoring und Möglichkeiten der Einflussnahme**

Unter Scoring versteht man die Bewertung eines einzelnen Ergebnis, die von Lucene selbst vorgenommen wird. Eine Ergebnisliste wird von Lucene automatisch nach diesen Scores sortiert, was unserer Relevanz-Sortierung entspricht.

Das Lucene Scoring hier im Detail zu erörtern, wäre mehr als müsig. Es gibt einen Grund, warum Lucene seit jeher die "explain" - Methode kennt, die eine detaillierte kaskadierte Auflistung aller Kriterien für eine Ergebnismenge liefert,  diese Auflistung aber eher mystifiziert. Gerade in Hinblick auf die verschiedenen Lucene-Versionen sollten wir auch weiterhin mit dieser blackbox leben.

Lohnenswerter erscheinen Boosts. Ich kann jedes Query mit einem Boost versehen. Beispiel (trivial):

~~~
Finde alle Einträge für 
	(Artikel ist "Unterrock" UND Farbe ist "rot", BOOST ist 15,0) ODER
	(Artikel ist "Unterrock" UND Farbe ist "weiß", BOOST ist 10,0)
~~~

Die roten Unterröcke kommen also auf jeden Fall zuerst (so es welche gibt), einfach weil der Boost-Faktor höher ist. Es kommen allerdings nur rote oder weiße Unterröcke, sonst nichts. Also warum nicht:

~~~
// Note: default boost ist 10,0 (10F)
Finde alle Einträge für 
	(Artikel ist "Unterrock", BOOST ist 25,0) ODER
	(Farbe ist "rot", BOOST ist 20,0) ODER
	(Farbe ist "weiß", BOOST ist 15,0)
~~~

Alle Boosts sind also höher als 10, wirken sich demnach fördernd aus. Es kommt also alles, was rot oder weiß ist (auch Schuhe, Socken etc.) und sämtliche Unterröcke, egal welcher Farbe. Aber es kommt eben richtig sortiert:

1. Rot-weiß karierte oder gestreifte Unterröcke
1. Rote Unterröcke
2. Weiße Unterröcke
3. Andere Unterröcke
4. Alles andere, was rot ist
5. Alles andere, was weiß ist

Schon besser, oder? Muss aber nicht zu 100% so kommen, da Lucene eben noch haufenweise andere Parameter beim Scoring berücksichtigt. Multipliziert man aber im Beispiel sämlichte expliziten Boosts mit 10, sollte man auf der sicheren Seite sein. Wenn immer noch nicht, dann eben mal 100.

---

**8. Machine Learning als Ansatz**

Was schrieb ich da gerade? *Wenn immer noch nicht, dann eben mal 100.* ??? Ist das mein Ernst? Wohl kaum.

Aber es ist gängige Praxis gerade bei kleineren Projekten. Irgendein Suchbegriff liefert im Livebetrieb seltsame Ergebnisse, also passt der Entwickler die Boosts an, bis es eben klappt. Und wundert sich, dass ihm plötzlich andere Suchbegriffe um die Ohren fliegen. Mal abgesehen davon, dass dem Entwickler ohnehin nur Test-Daten zur Verfügung stehen, mit denen in aller Regel wenig bis nichts anzufangen ist.

Die Geschichte endet nie, es sei denn, der Entwickler gibt auf (und sein Chef auch).

Aber warum das nicht die Maschine machen lassen. Wir haben doch alles, was wir brauchen

- Wir wissen, wie man im Live-Betrieb die Conversion Rate misst, also letztlich so etwas wie die Qualität der Suche
- Wir können mit Boosts operieren
- Wir haben uns verabschiedet von der Illusion, unsere Suche anhand von einzelnen Ausreißern zu verbessern. Schwarze Schafe lassen wir einfach ziehen.

**Noch einmal: wir können die Qualität einer Suche nicht beherrschen.** Und exakt da fängt Machine Learning an. Die Maschine findet Optimierungen aus einer Masse von Daten, die wir Menschen eben aufgrund deren Diversität nicht mehr herleiten können.

Die Maßnahmen:

- Wir entwickeln ein recht breites Query-Modell aus lauter geboosteten Queries. Diese sind mehrheitlich ODER - verknüpft.
- Die Boost-Faktoren machen wir dynamisch, gleichwohl gecached. Fuzzy Queries bekommen dabei einen kleineren Default Boost als z.B. Exact Queries.
- Wir sammeln die Klick-Daten der Nutzer wie in Punkt 6 beschrieben.
- Wir bringen der Maschine bei, zu schlechtes Nutzerverhalten durch Anpassungen von Boosts in einem vorgegebenen Rahmen zu korrigieren.
- Die Maschine muss vorherige Fehleinschätzungen erkennen und zurücknehmen können. 
- Wir sagen der Maschine, ab wann wir uns mit dem Nutzerverhalten zufriedengeben.

**ABER ACHTUNG, GEFAHR!**

So etwas ist angreifbar. Ganz einfach: ich schreibe einen Bot, der den lieben langen Tag nichts anderes macht, als Ergebnis an Position 100 als erstes zu klicken. Das vielleicht 100.000 mal pro Tag. Aber das wäre noch harmlos.

Schlimmer, wenn ich mir merke, was an Position 100 kam und dieses Ergebnis nun ständig als erstes klicke. Sollte mein Bot nicht irgendwann ausgesperrt werden, würde er die Suche zwingen, die Boost so zu verschieben, dass irgendwann mein auserwähltes Ergebnis an Position 1 erscheint. Ok, so schlimm kann es nicht kommen, aber mein Bot würde der Suche immerhin schaden.

- Also niemals der Maschine allzu große Freiheiten einräumen (min und max Boost)
- Das Query-Modell muss stabil genug sein. Bloß nicht den Boost zuviel Macht einräumen.
- Bots sollten ausgesperrt werden (time tracking)
- IP blacklisting
- Menschliche Intervention muss immer gewährleistet sein
- weitere Maßnahmen

---

**9. Best Practices**

- **Never use Lucene as a database.**  Das ist viel zu langsam, einfach weil zu aufgebläht. Daten gehören auf den Heap (bevorzugt Hashmap) oder in irgendeine Cassandra. Lucene nur als Index mit exakt einem stored field: eben dem PK zur Map oder Cassandra. Lucene sagt also, was kommen soll, der Content wird dann irgendwoher gezogen.
- **InMemory oder FS?** Klare Antwort: normalerweise jain, it depends. Bei einem schmalen Index InMemory, das auf jeden Fall. Sollte man Lucene als Datenbank missbrauchen, dann auf jeden Fall FS. Lucene hat einen gut funktionierenden Cache. Und der holt richtig schnell auf in den benchmarks. Den Lucene-Entwicklern ist außerdem nicht entgangen, was man alles mit FilePointers anstellen kann.
- **How Lucene updates an index.** Kann Lucene nicht. Lucene muss komplett neu indizieren (wie auch sonst?). Das geht zwar überraschend schnell, aber dennoch sollte man sich dessen bewusst sein.
- **Lucene as an observer or time based indexing.** Hat sich als prima erwiesen. Beispiel: alle n Sekunden geht Lucene an die Datenbank, holt sich alles, indiziert neu. Wenn der Vorgang zu Ende ist, wird der Pointer auf die neue Referenz gesetzt. Voraussetzung: der Index sollte klein bleiben. Also bitte Punkt 1 hier beachten. 

Es sollte übrigens bei einer Million Datensätze möglich sein, den Index jede Minute komplett neu aufzusetzen und dann einzuhängen. Dies hat sich jedenfalls als best practice erwiesen.

Disclaimer: möglicherweise macht ElasticSearch das bereits so. Kann ich mir aber nicht vorstellen nach allem, was ich auf deren Dokumentationen gelesen habe. 