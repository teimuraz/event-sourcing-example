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

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import tk.lib.eventsourcing.EventStore.EventStoreRecord
import tk.lib.eventsourcing.SingleShardProcessorActor.ProcessNextEvents
import tk.lib.pubsub.Producer
import play.api.Logging
import play.api.libs.json.{ Format, Json, Writes }

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import akka.pattern.after
import tk.lib.eventsourcing.EventStorePubsubConnector.Config
import akka.pattern.pipe
import tk.lib.core.futureutils.runSequentially
import tk.lib.pubsub.Producer.MessageOrdering

private[eventsourcing] class SingleShardProcessor[
    Id: Format,
    Evt: Format
](
    topic: String,
    shard: Int,
    eventStore: EventStore[Id, Evt],
    producer: Producer,
    pubsubConnectorOffsetManger: PubsubConnectorOffsetManger,
    val config: Config
)(
    implicit val ec: ExecutionContext,
    actorSystem: ActorSystem,
    messageWrites: Writes[EventStoreRecord[Id, Evt]]
) extends Logging {

  private val singleShardProcessorActor = actorSystem.actorOf(
    SingleShardProcessorActor.props(this)
  )

  def run(): Unit =
    singleShardProcessorActor ! ProcessNextEvents

  def processEvents(): Future[Long] = {
    val result = for {
      lastOffset <- pubsubConnectorOffsetManger.getLastOffset(topic, shard)
      eventStoreRecords <- eventStore
        .getEventsAfterOffset(lastOffset, shard)
      _ <- {
        val initial: (Long, Seq[() => Future[Boolean]]) =
          (lastOffset, Seq(() => Future.successful(true)))

        val (_, futures) =
          eventStoreRecords.foldLeft(initial)((acc, eventStoreRecord) => {
            val (lastOffset, functions) = acc
            val func = () => {
              publishEvent(eventStoreRecord).flatMap { _ =>
                pubsubConnectorOffsetManger
                  .updateLastOffset(
                    topic,
                    shard,
                    lastOffset,
                    eventStoreRecord.offset
                  )
              }
            }
            (eventStoreRecord.offset, functions :+ func)
          })

        runSequentially(futures)
      }
    } yield {
      eventStoreRecords.lastOption
        .map(_.offset)
        .getOrElse(lastOffset)
    }

    result.recoverWith {
      case e =>
        logger.error(
          s"Failed to process events, error: ${e.getMessage}",
          e
        )
        Future.failed(e)
    }
  }

  private implicit val messageOrdering: MessageOrdering[EventStoreRecord[Id, Evt]] =
    (message: EventStoreRecord[Id, Evt]) => Json.toJson(message.entityId).toString()

  private def publishEvent(
      eventStoreRecord: EventStoreRecord[Id, Evt]
  ): Future[String] = {

    logger.debug(s"Publishing event to topic $topic $eventStoreRecord")
    producer.publish(eventStoreRecord, topic)
  }

}

class SingleShardProcessorActor(
    singleShardProcessor: SingleShardProcessor[_, _]
) extends Actor
    with ActorLogging {
  implicit val ec: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case ProcessNextEvents =>
      after(singleShardProcessor.config.pollInterval, context.system.scheduler) {
        singleShardProcessor.processEvents()
      }.map(_ => ProcessNextEvents).recover {
        case e =>
          log.error(e, e.getMessage)
          ProcessNextEvents
      } pipeTo self
  }
}

object SingleShardProcessorActor {

  case object ProcessNextEvents

  def props(singleShardProcessor: SingleShardProcessor[_, _]) =
    Props(classOf[SingleShardProcessorActor], singleShardProcessor)

}
