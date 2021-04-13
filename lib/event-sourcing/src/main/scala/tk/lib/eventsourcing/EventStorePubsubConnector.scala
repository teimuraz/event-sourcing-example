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
import EventStorePubsubConnector.Components
import play.api.libs.json.{ Format, Json, OFormat, Writes }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import play.api.Logging
import tk.lib.eventsourcing.EventStore.EventStoreRecord
import tk.lib.pubsub.Producer
import reactivemongo.play.json.compat._
import tk.lib.core.json.Formats._

/**
  * Publish events from event store to the pubsub topic.
  * Polls event store with configurable interval.
  * Currently uses akka scheduler to run the job with given interval in the given instance of the app,
  * but should be replaced by some external scheduling tools, since multiple instances of this service can be running
  * (for example, the service can be deployed in kubernetes with several pods).
  *
  * Even tough jobs can run concurrently, it won't corrupt data, since concurrency issues are handled by connector
  * implementation.
  */
class EventStorePubsubConnector[
    Id: Format,
    Evt: Format
](
    val topic: String,
    eventStore: EventStore[Id, Evt],
    components: Components
)(implicit messageWrites: Writes[EventStoreRecord[Id, Evt]])
    extends Logging {

  private implicit val actorSystem: ActorSystem = components.actorSystem

  implicit def ec: ExecutionContext = actorSystem.dispatcher

  private val config = components.config

  def run() = {

    // Since multiple instances of this service are running in the production
    // We set random initial delay to avoid simultaneous job run across instances.
    // (sure, there is still the chance that job will be run simultaneously, but it's not that critical).

    val initialDelay = if (config.runWithRandomInitialDelay) {
      val random = scala.util.Random
      random.nextInt(1000) * 3.milliseconds
    } else {
      0.milliseconds
    }

    Range(0, eventStore.numShards).foreach { shard =>
      logger.info(
        s"Running event sourcing pubsub connector with id $topic, shard: $shard with initial delay $initialDelay"
      )

      val singleShardProcessor =
        new SingleShardProcessor[Id, Evt](
          topic,
          shard,
          eventStore,
          components.producer,
          components.pubsubConnectorOffsetManger,
          config
        )

      singleShardProcessor.run()
    }
  }
}

object EventStorePubsubConnector {
  case class Components(
      producer: Producer,
      actorSystem: ActorSystem,
      config: Config,
      pubsubConnectorOffsetManger: PubsubConnectorOffsetManger
  )

  case class Config(pollInterval: FiniteDuration, runWithRandomInitialDelay: Boolean)

  object Config {
    implicit val format: OFormat[Config] = Json.format[Config]
  }
}
