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

trait EventSourcedRepository extends BaseEventSourcedRepository {
  def components: EventSourcedRepositoryComponents
  def collectionName: String
  def numShards: Int

  override lazy val idGenerator: NumericIdGenerator = components.idGenerator

  override lazy val eventStore: EventStore[entityBehaviour.Id, entityBehaviour.Evt] =
    new EventStore[entityBehaviour.Id, entityBehaviour.Evt](
      collectionName,
      components.database,
      components.eventShardComputer,
      components.dateTimeUtils,
      components.offsetGenerator,
      numShards,
      idMongoDbFormat,
      eventMongoDbFormat
    )
}
