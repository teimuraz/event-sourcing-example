/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing

import akka.actor.ActorSystem
import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.eventsourcing.EventStore.EventStoreRecord
import tk.lib.eventsourcing.UserProjectionRepository.UserProjection
import tk.lib.eventsourcing.UserEvent.{ UserActivated, UserCreated, UsernameChanged }
import tk.lib.pubsub.AckReply
import tk.lib.pubsub.AckReply.Ack
import play.api.Logging

import scala.concurrent.{ ExecutionContext, Future }

class UserEventConsumer(
    val googleConfig: GoogleGeneralConfig,
    userProjectionRepository: UserProjectionRepository
)(implicit val system: ActorSystem, val ec: ExecutionContext)
    extends EventStoreRecordConsumer[UserId, UserEvent]
    with Logging {

  override def subscription: String = "userEventsTopic_userProjectionsSubscription"

  override def handle(message: EventStoreRecord[UserId, UserEvent]): Future[AckReply] = {
    logger.info(s"Received $message")
    val result = message.event match {
      case e: UserCreated =>
        val userProjection =
          UserProjection(
            e.entityId,
            e.username,
            e.email,
            e.status,
            tk.lib.core.model.Metadata.empty
          )

        userProjectionRepository.store(userProjection)
      case e: UserActivated =>
        for {
          user <- userProjectionRepository.find(e.entityId).map(_.get)
          updated = user.copy(status = e.status)
          _ <- userProjectionRepository.store(updated)
        } yield {
          updated
        }
      case e: UsernameChanged =>
        for {
          user <- userProjectionRepository.find(e.entityId).map(_.get)
          updated = user.copy(username = e.username)
          _ <- userProjectionRepository.store(updated)
        } yield {
          updated
        }

    }

    result.map(_ => Ack)
  }
}
