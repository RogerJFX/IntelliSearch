package de.crazything.search

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{Duration, FiniteDuration}

trait MagicSettings {
  val MAGIC_NUM_DEFAULT_HITS = 100
  val MAGIC_NUM_DEFAULT_HITS_FILTERED = 500
  val DEFAULT_TIMEOUT: FiniteDuration = Duration.create(1, TimeUnit.DAYS)

  val DEFAULT_DIRECTORY_NAME = "DEFAULT_DIRECTORY"
}
