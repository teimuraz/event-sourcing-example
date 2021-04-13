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

import tk.lib.core.datetime.DateTimeUtils
import tk.lib.core.error.InternalErrorException
import reactivemongo.api.{ Cursor, DB, ReadPreference }
import reactivemongo.api.bson.collection.BSONCollection
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import tk.lib.core.tryutils.syntax._
import play.api.libs.json.{
  Format,
  JsError,
  JsObject,
  JsResult,
  JsSuccess,
  JsValue,
  Json,
  OFormat
}
import reactivemongo.play.json.compat._
import json2bson.{ toDocumentReader, toDocumentWriter }

import java.time.Instant
import tk.lib.core.futureutils._
import tk.lib.eventsourcing.EventStore.{ EventStoreRecord, NewEventStoreRecord }
import tk.lib.eventsourcing.EventStore.EventStoreRecordMongoDbFormat.{
  entityIdField,
  entityVersionField,
  offsetField,
  shardField
}
import reactivemongo.api.indexes.{ Index, IndexType }

import java.util.concurrent.atomic.AtomicBoolean

/**
  * @param collectionName
  * @param database
  * @param eventShardComputer
  * @param numShards
  *   Number of shards in which events will be distributed and published to the pubsub topic in parallel.
  *   Each shard will be handled by dedicated event processor (pubsub connector)
  *   The more shards, the faster events will be published, but number of shards increases
  *   number of connectors which poll event store.
  *   Once set, numShards cannot be changed, otherwise it can break events ordering in PubSub.
  *   (At least it will require manual work to redistribute events across new number of shards).
  * @param ec
  */
class EventStore[Id, Evt](
    collectionName: String,
    database: DB,
    eventShardComputer: EventShardComputer,
    dateTimeUtils: DateTimeUtils,
    offsetGenerator: OffsetGenerator,
    val numShards: Int,
    idMongoDbFormat: Format[Id],
    eventMongoDbFormat: Format[Evt]
)(
    implicit ec: ExecutionContext
) {

  def collection: BSONCollection =
    database.collection(collectionName)

  private implicit val idFormatImplicit: Format[Id]     = idMongoDbFormat
  private implicit val eventFormatImplicit: Format[Evt] = eventMongoDbFormat

  private val prepared: AtomicBoolean = new AtomicBoolean()

  private def prepareIfNeeded(): Future[Boolean] =
    if (prepared.getAndSet(true)) {
      Future.successful(true)
    } else {
      val indexesFuture = Seq(
        Index(
          key = Seq(
            (entityIdField, IndexType.Ascending),
            (entityVersionField, IndexType.Ascending)
          ),
          unique = true
        ),
        Index(
          key = Seq((offsetField, IndexType.Ascending), (shardField, IndexType.Ascending))
        )
      ).map { index => () =>
        collection.indexesManager.ensure(index)
      }
      runSequentially(indexesFuture).map(_ => true)
    }

  def getEventsOfEntity(
      id: Id
  ): Future[Seq[EventStoreRecord[Id, Evt]]] = {
    val selector = Json.obj(entityIdField -> id)
    findAllBySelector(selector)
  }

  /**
    * TODO:: Stream events or set limit
    */
  def getEventsAfterOffset(
      offset: Long,
      shard: Int
  ): Future[Seq[EventStoreRecord[Id, Evt]]] = {
    val selector =
      Json.obj(offsetField -> Json.obj("$gt" -> offset), shardField -> shard)
    findAllBySelector(selector)
  }

  private def findAllBySelector(
      selector: JsObject
  ): Future[Seq[EventStoreRecord[Id, Evt]]] =
    for {
      _ <- prepareIfNeeded()
      docs <- collection
        .find(selector)
        .cursor[JsObject](ReadPreference.primary)
        .collect[Seq](-1, Cursor.FailOnError[Seq[JsObject]]())
      eventStoreRecords <- Future.fromTry(
        Try.fromSeqOfTries(docs.map(doc => docToEventStoreRecord(doc)))
      )
      eventStoreRecordsSorted = eventStoreRecords.sortBy(_.offset)
    } yield {
      eventStoreRecordsSorted
    }

  private def docToEventStoreRecord(
      doc: JsObject
  ): Try[EventStoreRecord[Id, Evt]] = {
    import tk.lib.eventsourcing.EventStore.EventStoreRecordMongoDbFormat._
    doc.validate[EventStoreRecord[Id, Evt]] match {
      case JsSuccess(value, _) => Success(value)
      case JsError(errors) =>
        Failure(
          new InternalErrorException(s"Failed to decode event $doc, errors: $errors")
        )
    }
  }

  def storeEvents(
      newEventStoreRecords: Seq[NewEventStoreRecord[Id, Evt]]
  ): Future[Boolean] = {
    import tk.lib.eventsourcing.EventStore.EventStoreRecordMongoDbFormat._
    val eventStoreDocumentFutures = newEventStoreRecords.map {
      newEventStoreRecord =>
        () =>
          // TODO:: Optimize it, now performs generation  for each event, may be do it in batch?
          offsetGenerator.nextOffset(collectionName).map { offset =>
            val shard =
              eventShardComputer.computeShard(newEventStoreRecord.entityId, numShards)
            val record = EventStoreRecord(
              offset,
              newEventStoreRecord.entityId,
              newEventStoreRecord.version,
              shard,
              dateTimeUtils.now,
              newEventStoreRecord.event
            )
            Json.toJsObject(record)
          }
    }

    for {
      _                   <- prepareIfNeeded()
      eventStoreDocuments <- runSequentially(eventStoreDocumentFutures)
      dbWithSession       <- database.startSession()
      dbWithTx            <- dbWithSession.startTransaction(None)
      _ <- collection.insert(ordered = true).many(eventStoreDocuments).recoverWith {
        case e if e.getMessage.contains("E11000 duplicate key error collection") =>
          // Event's entityId and entityVersion compose unique index in event store, so if the event of given entity with
          // given entity version already exists, that means the entity was modified concurrently by another
          // process(es). Raise concurrency exception.
          dbWithTx.abortTransaction().flatMap(_ => dbWithSession.endSession()).flatMap {
            _ =>
              Future.failed(
                new ConcurrencyException(
                  s"Entity was modified by another process.",
                  Some(e)
                )
              )
          }
      }
      _ <- dbWithTx.commitTransaction()
      _ <- dbWithSession.endSession()
    } yield {
      true
    }
  }

  /**
    * Mark event store as unprepared (e.g., force rebuild indexes) - used in tests.
    */
  def markAsUnprepared(): Unit =
    prepared.set(false)
}

object EventStore {

  /**
    * IMPORTANT: entityId and entityVersion should compose unique index in underlying implementation to work properly.
    * (Event store creates them automatically)
    */
  case class EventStoreRecord[Id, Evt](
      offset: Long,
      entityId: Id,
      entityVersion: Int,
      shard: Int,
      time: Instant,
      event: Evt
  )

  object EventStoreRecordMongoDbFormat {

    val offsetField        = "_id"
    val entityIdField      = "entityId"
    val entityVersionField = "entityVersion"
    val shardField         = "shard"
    val timeField          = "time"
    val eventField         = "event"

    import tk.lib.core.mongo.json.Formats._

    implicit def format[Id: Format, Evt: Format]: OFormat[EventStoreRecord[Id, Evt]] =
      new OFormat[EventStoreRecord[Id, Evt]] {
        override def reads(json: JsValue): JsResult[EventStoreRecord[Id, Evt]] =
          for {
            offset   <- (json \ offsetField).validate[Long]
            entityId <- (json \ entityIdField).validate[Id]
            version  <- (json \ entityVersionField).validate[Int]
            shard    <- (json \ shardField).validate[Int]
            time <- (json \ timeField)
              .validate[Instant]
            event <- (json \ eventField).validate[Evt]
          } yield {
            EventStoreRecord(offset, entityId, version, shard, time, event)
          }

        override def writes(o: EventStoreRecord[Id, Evt]): JsonTime = Json.obj(
          offsetField        -> Json.toJson(o.offset),
          entityIdField      -> Json.toJson(o.entityId),
          entityVersionField -> Json.toJson(o.entityVersion),
          shardField         -> Json.toJson(o.shard),
          timeField          -> Json.toJson(o.time),
          eventField         -> Json.toJson(o.event)
        )
      }
  }

  object EventStoreRecordDefaultFormat {
    implicit def format[Id: Format, Evt: Format]: OFormat[EventStoreRecord[Id, Evt]] =
      Json.format
  }

  case class NewEventStoreRecord[Id, Evt](
      entityId: Id,
      version: Int,
      event: Evt
  )

}
