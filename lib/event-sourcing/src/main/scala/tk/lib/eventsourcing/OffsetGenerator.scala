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

package tk.lib.eventsourcing

import tk.lib.eventsourcing.OffsetGenerator.OffsetDescriptor
import play.api.libs.json.{ JsObject, JsResult, JsValue, Json, OFormat }
import reactivemongo.api.DB
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.{ ExecutionContext, Future }
import reactivemongo.play.json.compat._
import json2bson.{ toDocumentReader, toDocumentWriter }

/**
  * Generate event offset
  */
class OffsetGenerator(database: DB)(implicit ec: ExecutionContext) {

  private def collection: BSONCollection =
    database.collection("offsets")

  def nextOffset(forKey: String): Future[Long] = {
    val selector = Json.obj("_id"  -> forKey)
    val update   = Json.obj("$inc" -> Json.obj("offset" -> 1))
    collection
      .findAndUpdate(selector, update, fetchNewObject = true, upsert = true)
      .map(_.result[OffsetDescriptor])
      .map(_.map(_.offset).getOrElse(1))
  }
}

object OffsetGenerator {
  case class OffsetDescriptor(key: String, offset: Long)

  object OffsetDescriptor {
    implicit val format: OFormat[OffsetDescriptor] = new OFormat[OffsetDescriptor] {
      override def reads(json: JsValue): JsResult[OffsetDescriptor] =
        for {
          key    <- (json \ "_id").validate[String]
          offset <- (json \ "offset").validate[Long]
        } yield {
          OffsetDescriptor(key, offset)
        }

      override def writes(o: OffsetDescriptor): JsObject = Json.obj(
        "_id"    -> o.key,
        "offset" -> o.offset
      )
    }
  }
}
