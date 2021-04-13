package query.user

import akka.actor.ActorSystem
import tk.lib.core.auth.api.UserId
import tk.lib.core.datetime.DateTimeUtils
import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.eventsourcing.EventStore.EventStoreRecord
import tk.lib.eventsourcing.EventStoreRecordConsumer
import tk.lib.pubsub.AckReply
import tk.lib.pubsub.AckReply.Ack
import domain.user.{ UserCreated, UserEvent }
import play.api.Logging
import tk.lib.core.model.Metadata
import config.GoogleConfig.PubsubConfig
import scala.concurrent.{ ExecutionContext, Future }
import jsonformat.DefaultFormats._

class UserEventConsumer(
    pubsubConfig: PubsubConfig,
    userProjectionRepository: UserProjectionRepository,
    val googleConfig: GoogleGeneralConfig,
    val dateTimeUtils: DateTimeUtils
)(
    implicit val ec: ExecutionContext,
    val actorSystem: ActorSystem
) extends EventStoreRecordConsumer[UserId, UserEvent]
    with Logging {

  override def subscription: String =
    pubsubConfig.userEventsTopic_userProjectionsSubscription

  override def handle(
      message: EventStoreRecord[UserId, UserEvent]
  ): Future[AckReply] =
    message.event match {
      case e: UserCreated =>
        handle(e, message)
    }

  def handle(
      e: UserCreated,
      message: EventStoreRecord[UserId, UserEvent]
  ): Future[AckReply] =
    userProjectionRepository.find(e.entityId).flatMap {
      case Some(_) =>
        logger.warn(s"User projection already exists. Ignoring. $e")
        Future.successful(AckReply.Ack)
      case None =>
        userProjectionRepository
          .store(
            UserProjection(
              id = e.entityId,
              email = e.email,
              firstName = e.firstName,
              lastName = e.lastName,
              oauthUserId = e.oauthUserId,
              createdAt = e.createdAt,
              latestUserEntityVersion = message.entityVersion,
              systemManager = None,
              latestSystemManagerEntityVersion = 0,
              Metadata.empty
            )
          )
          .map(_ => AckReply.Ack)
    }

}
