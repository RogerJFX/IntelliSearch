// call: sbt scalastyle
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// call: sbt clean coverage test
// then call: sbt coverageReport
// results will appear in /target/scala-2.12/scoverage-report/[whatever-you-like-there].html
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")