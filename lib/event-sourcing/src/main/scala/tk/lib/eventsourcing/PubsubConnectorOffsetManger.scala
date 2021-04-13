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

import tk.lib.core.futureutils.runSequentially
import tk.lib.core.model.ConcurrencyException
import tk.lib.eventsourcing.PubsubConnectorOffsetManger.PubsubConnectorDescriptor
import play.api.libs.json.{ JsObject, JsResult, JsValue, Json, OFormat }
import reactivemongo.api.{ DB, ReadPreference }
import reactivemongo.api.bson.collection.BSONCollection
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ ExecutionContext, Future }
import reactivemongo.api.indexes.{ Index, IndexType }
import reactivemongo.play.json.compat.json2bson
import reactivemongo.play.json.compat._
import json2bson.{ toDocumentReader, toDocumentWriter }

class PubsubConnectorOffsetManger(database: DB)(implicit ec: ExecutionContext) {

  private def collection: BSONCollection =
    database.collection("pubsubConnectors")

  private val prepared: AtomicBoolean = new AtomicBoolean()

  private def prepareIfNeeded(): Future[Boolean] =
    if (prepared.getAndSet(true)) {
      Future.successful(true)
    } else {
      val indexesFuture = Seq(
        Index(
          key = Seq(
            ("topic", IndexType.Ascending),
            ("shard", IndexType.Ascending)
          ),
          unique = true
        )
      ).map { index => () =>
        collection.indexesManager.ensure(index)
      }
      runSequentially(indexesFuture).map(_ => true)
    }

  def getLastOffset(topic: String, shard: Int): Future[Long] = {
    val selector = Json.obj("topic" -> topic, "shard" -> shard)
    for {
      _ <- prepareIfNeeded()
      lastOffset: Long <- collection
        .find(selector)
        .one[PubsubConnectorDescriptor](ReadPreference.primary)
        .map(_.map(_.lastOffset).getOrElse(0L))
    } yield {
      lastOffset
    }
  }

  def updateLastOffset(
      topic: String,
      shard: Int,
      previousLastOffset: Long,
      lastOffset: Long
  ): Future[Boolean] = {

    val selector = Json.obj(
      "topic"      -> topic,
      "shard"      -> shard,
      "lastOffset" -> previousLastOffset
    )

    val modifier = Json.obj(
      "$set" -> Json.obj(
        "topic"      -> topic,
        "shard"      -> shard,
        "lastOffset" -> lastOffset
      )
    )

    for {
      // Update lastOffset with optimistic lock.
      // Try to find the connector descriptor with given id, shard and original last offset (`previousLastOffset).
      // If connector with given conditions doesn't exist that means that either the connector descriptor itself
      // doesn't exist (in this case it will try to create new connector descriptor (upsert = true))
      // or it has been modified concurrently by another process.
      _ <- prepareIfNeeded()
      _ <- collection.update
        .one(q = selector, u = modifier, upsert = true, multi = false)
        .recoverWith {
          case e if e.getMessage.contains("E11000 duplicate key error collection") =>
            // Mongodb did not find document with connector descriptor's id, shard and original last offset
            // and therefore tried to create new connector descriptor with given id and shard (upsert=true),
            // but since upsert/ failed with duplicate key violation (id and shard compose unique index)
            // that means that connector descriptor with given id and shard actually exists but with
            // different lastOffset.
            // So indeed, the connector descriptor has been modified concurrently by another process.
            // Raise ConcurrencyException.
            Future.failed(
              new ConcurrencyException(
                s"Connector descriptor with id $topic has been " +
                s"modified concurrently by another process. Connector id: $topic, shard: $shard," +
                s" previousLastOffset: $previousLastOffset, lastOffset: $lastOffset",
                Some(e)
              )
            )
        }
    } yield {
      true
    }
  }
}

object PubsubConnectorOffsetManger {
  case class PubsubConnectorDescriptor(
      topic: String,
      shard: Int,
      lastOffset: Long
  )

  object PubsubConnectorDescriptor {

    implicit val format: OFormat[PubsubConnectorDescriptor] =
      Json.format[PubsubConnectorDescriptor]
  }
}
