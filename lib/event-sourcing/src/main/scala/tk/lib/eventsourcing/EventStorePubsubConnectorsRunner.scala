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
import tk.lib.core.error.ValidationException
import EventStorePubsubConnector.Config
import play.api.Logging

import scala.concurrent.ExecutionContext
import scala.util.Try

class EventStorePubsubConnectorsRunner private (
    actorSystem: ActorSystem,
    config: Config,
    private val connectorsAreRunning: Boolean,
    val connectors: Seq[EventStorePubsubConnector[_, _]] = Nil
)(implicit ec: ExecutionContext)
    extends Logging {

  def addConnector(
      connector: EventStorePubsubConnector[_, _]
  ): Try[EventStorePubsubConnectorsRunner] = Try {
    if (connectorsAreRunning) {
      throw new ValidationException(
        "Cannot add new connector after connectors were ran"
      )
    }

    if (connectors.exists(_.topic == connector.topic)) {
      throw new ValidationException(
        s"Connector with id ${connector.topic} is already added"
      )
    }

    copy(connectors = connectors :+ connector)
  }

  def copy(
      actorSystem: ActorSystem = actorSystem,
      config: Config = config,
      connectorsAreRunning: Boolean = connectorsAreRunning,
      connectors: Seq[EventStorePubsubConnector[_, _]] = connectors
  )(implicit ec: ExecutionContext = ec): EventStorePubsubConnectorsRunner =
    new EventStorePubsubConnectorsRunner(
      actorSystem,
      config,
      connectorsAreRunning,
      connectors
    )

  def run(): EventStorePubsubConnectorsRunner = {
    // Since multiple instances of this service are running in the production
    // We set random initial delay to avoid simultaneous job run across instances.
    // (sure, there is still the chance that job will be run simultaneously, but it's not that critical).
    if (!connectorsAreRunning) {
      doRunProcessors()
    } else {
      logger.warn("Pubsub connectors are already running")
    }

    copy(connectorsAreRunning = true)
  }

  private def doRunProcessors(): Unit = {
    logger.info(s"Running ${connectors.size} event store pubsub connector(s)")

    connectors.foreach { connector =>
      connector.run()
    }
  }
}

object EventStorePubsubConnectorsRunner {
  def apply(
      actorSystem: ActorSystem,
      readSideProcessorConfig: Config,
      processors: Seq[EventStorePubsubConnector[_, _]] = Nil
  )(implicit ec: ExecutionContext): EventStorePubsubConnectorsRunner =
    new EventStorePubsubConnectorsRunner(
      actorSystem,
      readSideProcessorConfig,
      false,
      processors
    )
}
