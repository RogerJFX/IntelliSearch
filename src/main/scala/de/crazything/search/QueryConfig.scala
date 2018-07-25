package de.crazything.search

trait QueryConfig {

  protected object QueryEnabled {
    val TERM = 1
    val REGEX = 2
    val PHONETIC = 4
    val FUZZY = 8

    val ALL: Int = TERM | REGEX | PHONETIC | FUZZY
  }

  protected object Boost {
    val TERM = 10
    val REGEX = 7
    val PHONETIC = 4
    val FUZZY = 1
  }

  val FUZZY_MAX_EDITS = 2

  protected val PHONETIC = "PH"

}
