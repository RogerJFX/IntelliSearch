package de.crazything.search

trait FieldModify {

  private val replacements = Seq[(String, String)](
    ("Ä", "AE"),
    ("Ö", "OE"),
    ("Ü", "UE"),
    ("ß", "SS")
    )

  private val replaceChars: (String) => String = (str) => {
    var s = str
    replacements.foreach(r => s = s.replaceAll(r._1, r._2))
    s
  }
  // tmp switched off
  val prepareField: (String) => String = (str) => str // replaceChars(str.trim.toUpperCase)
}
