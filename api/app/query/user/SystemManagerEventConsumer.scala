package query.user

import akka.actor.ActorSystem
import tk.lib.core.auth.api.UserId
import tk.lib.core.datetime.DateTimeUtils
import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.eventsourcing.EventStore.EventStoreRecord
import tk.lib.eventsourcing.EventStoreRecordConsumer
import tk.lib.pubsub.AckReply
import play.api.Logging
import domain.systemmanager.{
  SystemManagerCreated,
  SystemManagerEvent,
  SystemManagerState
}
import config.GoogleConfig.PubsubConfig
import scala.concurrent.{ ExecutionContext, Future }
import jsonformat.DefaultFormats._

class SystemManagerEventConsumer(
    pubsubConfig: PubsubConfig,
    userProjectionRepository: UserProjectionRepository,
    val googleConfig: GoogleGeneralConfig,
    val dateTimeUtils: DateTimeUtils
)(
    implicit val ec: ExecutionContext,
    val actorSystem: ActorSystem
) extends EventStoreRecordConsumer[UserId, SystemManagerEvent]
    with Logging {

  override def subscription: String =
    pubsubConfig.systemManagerEventsTopic_userProjectionsSubscription

  override def handle(
      message: EventStoreRecord[UserId, SystemManagerEvent]
  ): Future[AckReply] =
    message.event match {
      case e: SystemManagerCreated => handle(message, e)
    }

  private def handle(
      message: EventStoreRecord[UserId, SystemManagerEvent],
      e: SystemManagerCreated
  ): Future[AckReply] =
    userProjectionRepository.find(e.entityId).flatMap {
      case None =>
        logger
          .warn(s"No user ${e.entityId} found thought it should already be created. $e")
        Future.successful(AckReply.Nack)
      case Some(userProjection) =>
        if (message.entityVersion <= userProjection.latestSystemManagerEntityVersion) {
          logger.warn(
            s"Event already handled. Ignoring. $userProjection, event: $message"
          )
          Future.successful(AckReply.Ack)
        } else {
          val updatedProjection = userProjection.copy(
            systemManager = Some(SystemManagerState(e.entityId, e.roles, e.createdAt)),
            latestSystemManagerEntityVersion = message.entityVersion
          )
          userProjectionRepository
            .store(updatedProjection)
            .map(_ => AckReply.Ack)
        }
    }

}
