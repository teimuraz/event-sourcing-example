/*
 *
 * Copyright 2021 TK
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package tk.lib.core.json

import tk.lib.core.datetime.DateTimeUtils

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success, Try }
import play.api.libs.json._
import tk.lib.core.datetime.syntax._

import java.time.Instant

trait Formats {

  implicit lazy val instantFormat: Format[Instant] = new Format[Instant] {
    override def writes(o: Instant): JsValue = JsString(o.toIsoString)

    override def reads(json: JsValue): JsResult[Instant] =
      for {
        value <- json.validate[String]
        instant <- DateTimeUtils.parseInstantTried(value) match {
          case Success(instant) => JsSuccess(instant)
          case Failure(e)       => JsError(e.getMessage)
        }
      } yield {
        instant
      }
  }

  implicit lazy val scalaFiniteDurationWrites: Writes[FiniteDuration] =
    (o: FiniteDuration) => JsString(o.toString)

  implicit lazy val scalaFiniteDurationReads: Reads[FiniteDuration] =
    (json: JsValue) => {
      for {
        durationStr <- json.validate[String]
        duration <- {
          val parts = durationStr.split(" ").toList
          parts match {
            case length :: unit :: Nil =>
              Try(FiniteDuration(length.toLong, unit)) match {
                case Success(d)         => JsSuccess(d)
                case Failure(exception) => JsError(exception.getMessage)
              }
            case _ => JsError(s"Invalid finite duration format: $durationStr")
          }
        }
      } yield {
        duration
      }
    }

  implicit lazy val scalaFiniteDurationFormat: Format[FiniteDuration] =
    Format(scalaFiniteDurationReads, scalaFiniteDurationWrites)

  implicit lazy val charFormat: Format[Char] = new Format[Char] {
    override def reads(json: JsValue): JsResult[Char] =
      json.validate[String].map(_.charAt(0))

    override def writes(o: Char): JsValue = JsString(o.toString)
  }

  implicit lazy val trimStringReads: Reads[String] = Reads.StringReads.map(_.trim)
}

object Formats extends Formats
