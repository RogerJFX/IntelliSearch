package de.crazything.app.itest

trait Network {

  def urlFromUriBase(uri: String): String = s"http://127.0.0.1:9001/$uri"

  def urlFromUriSocial(uri: String): String = s"http://127.0.0.1:9002/$uri"

}
