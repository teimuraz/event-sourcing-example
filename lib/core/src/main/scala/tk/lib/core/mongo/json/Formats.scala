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

package tk.lib.core.mongo.json

import play.api.libs.json.{ Format, JsResult, JsValue }
import reactivemongo.api.bson.BSONDateTime
import java.time.Instant
import reactivemongo.play.json.compat._
import bson2json._

trait Formats {
  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    override def reads(json: JsValue): JsResult[Instant] =
      json.validate[BSONDateTime].map(t => Instant.ofEpochMilli(t.value))

    override def writes(o: Instant): JsValue =
      BSONDateTime(o.toEpochMilli)
  }
}

object Formats extends Formats
