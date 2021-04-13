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

import akka.actor.ActorSystem
import akka.stream.Materializer
import tk.lib.core.CoreComponents
import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.pubsub.{ Producer, PubsubAdmin }
import com.softwaremill.macwire.wire
import reactivemongo.api.DB

import scala.concurrent.ExecutionContext

trait EventSourcingComponents extends CoreComponents {
  implicit def actorSystem: ActorSystem
  implicit def materializer: Materializer
  implicit def executionContext: ExecutionContext
  def database: DB

  def googleGeneralConfig: GoogleGeneralConfig

  lazy val eventShardComputer: EventShardComputer = wire[EventShardComputer]

  def pubsubConnectorOffsetManger: PubsubConnectorOffsetManger =
    wire[PubsubConnectorOffsetManger]

  def eventStorePubsubConnectorConfig: EventStorePubsubConnector.Config

  lazy val producer =
    new Producer(googleGeneralConfig)

  lazy val eventStorePubsubConnectorComponents: EventStorePubsubConnector.Components =
    EventStorePubsubConnector.Components(
      producer,
      actorSystem,
      eventStorePubsubConnectorConfig,
      pubsubConnectorOffsetManger
    )
}
