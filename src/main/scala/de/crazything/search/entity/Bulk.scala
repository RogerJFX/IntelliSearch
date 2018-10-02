package de.crazything.search.entity

import play.api.libs.json._

/**
  * Just for JSON serializing.
  *
  * @param entries Sequence of Type T
  * @tparam T Type of entries.
  */
case class Bulk[T](entries: Seq[T])

object Bulk {

  implicit def format[T](implicit fmt: Format[T]): OFormat[Bulk[T]] =
    new OFormat[Bulk[T]] {
      def reads(json: JsValue): JsSuccess[Bulk[T]] = JsSuccess(Bulk[T](
        (json \ "entries").as[Seq[T]]
      ))

      def writes(e: Bulk[T]) = JsObject(Seq(
        "entries" -> Json.toJson(e.entries)
      ))
    }
}
