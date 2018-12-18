package de.crazything.app.test.ml

import java.net.URL
import java.nio.file.{Files, Path, Paths}

import de.crazything.app.helpers.DataProvider
import de.crazything.search.ml.tuning.TunerConfig
import de.crazything.service.QuickJsonParser
import org.scalatest.FlatSpec

class ConfigTest extends FlatSpec with QuickJsonParser {

  "TuningConfig" should "read JSON to case class" in {
    import scala.collection.JavaConverters._
    val url: URL = DataProvider.getClass.getResource("/conf/complexSlogan.tuning.json")
    val path: Path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path).asScala
    val complete = lines.mkString("\n")
    val confClass = jsonString2T[TunerConfig](complete)
    assert(confClass.dedicated.length == 3)
  }

}
