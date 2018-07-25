// call: sbt scalastyle
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// call: sbt coverage test
// results will appear in /target/scoverage-report/[whatever-you-like-there].html
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")