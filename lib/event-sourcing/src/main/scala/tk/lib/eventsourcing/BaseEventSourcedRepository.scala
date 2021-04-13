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

import tk.lib.eventsourcing.idgenerator.NumericIdGenerator
import tk.lib.eventsourcing.EventStore.NewEventStoreRecord
import play.api.libs.json.Format
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.runtime.universe._

trait BaseEventSourcedRepository {
  val entityBehaviour: EntityBehaviour

  implicit def ec: ExecutionContext
  def idGenerator: NumericIdGenerator
  def eventStore: EventStore[entityBehaviour.Id, entityBehaviour.Evt]

  def idMongoDbFormat: Format[entityBehaviour.Id]
  def eventMongoDbFormat: Format[entityBehaviour.Evt]

//  implicit val idTypeTag: TypeTag[entityBehaviour.Id]

  def find(
      id: entityBehaviour.Id
  ): Future[Option[entityBehaviour.Entity]] =
    eventStore.getEventsOfEntity(id).map {
      case Nil => None
      case eventStoreRecords =>
        val state = eventStoreRecords
          .foldLeft(entityBehaviour.emptyState)((currentState, eventStoreRecord) => {
            currentState.applyEvent(eventStoreRecord.event)
          })
        Some(
          entityBehaviour.Entity(
            state,
            Metadata(
              Nil,
              eventStoreRecords.last.entityVersion,
              eventStoreRecords.last.entityVersion
            )
          )
        )
    }

  /**
    * Store the entity.
    * The implementation protects from storing concurrently modified entities (with help of event store).
    * So if multiple processes try to store given entity simultaneously, only one of process will succeed, other will
    * fail with concurrency exception.
    */
  def store(
      entity: entityBehaviour.Entity
  ): Future[entityBehaviour.Entity] =
    if (entity.metadata.uncommittedChanges.nonEmpty) {
      val eventStoreRecords = entity.metadata.uncommittedChanges.zipWithIndex.map {
        case (event, index) =>
          NewEventStoreRecord(
            entity.state.id,
            entity.metadata.persistedVersion + index + 1,
            event
          )
      }
      eventStore.storeEvents(eventStoreRecords).map { _ =>
        val updatedMetadata: Metadata[entityBehaviour.Evt] = entity.metadata
          .copy(
            uncommittedChanges = Nil,
            persistedVersion = entity.metadata.currentVersion
          )
        entity.copy(metadata = updatedMetadata)

      }
    } else {
      Future.successful(entity)
    }

  /**
    * Generate id.
    * Please note that underlying value of Id should be of type Long
    */
  def nextId()(
      implicit typeTag: TypeTag[entityBehaviour.Id]
  ): Future[entityBehaviour.Id] = idGenerator.generate[entityBehaviour.Id]()
}
