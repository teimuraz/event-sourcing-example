package query.organization

import akka.actor.ActorSystem
import tk.lib.core.datetime.DateTimeUtils
import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.core.model.Metadata
import tk.lib.eventsourcing.EventStore.EventStoreRecord
import tk.lib.eventsourcing.EventStoreRecordConsumer
import tk.lib.pubsub.AckReply
import domain.organization.OrganizationId
import domain.organization.{
  MemberAddedToOrganization,
  OrganizationCreated,
  OrganizationEvent
}
import config.GoogleConfig.PubsubConfig
import play.api.Logging
import jsonformat.DefaultFormats._
import scala.concurrent.{ ExecutionContext, Future }

class OrganizationEventConsumer(
    pubsubConfig: PubsubConfig,
    val googleConfig: GoogleGeneralConfig,
    val dateTimeUtils: DateTimeUtils,
    organizationProjectionRepository: OrganizationProjectionRepository
)(
    implicit val ec: ExecutionContext,
    val actorSystem: ActorSystem
) extends EventStoreRecordConsumer[OrganizationId, OrganizationEvent]
    with Logging {

  override def subscription: String =
    pubsubConfig.organizationEventsTopic_organizationProjectionsSubscription

  override def handle(
      message: EventStoreRecord[OrganizationId, OrganizationEvent]
  ): Future[AckReply] =
    message.event match {
      case e: OrganizationCreated       => handle(e, message)
      case e: MemberAddedToOrganization => handle(e, message)
    }

  def handle(
      e: OrganizationCreated,
      message: EventStoreRecord[OrganizationId, OrganizationEvent]
  ): Future[AckReply] =
    organizationProjectionRepository.find(e.entityId).flatMap {
      case Some(_) =>
        logger.warn(s"Organization projection already exists. Ignoring. $e")
        Future.successful(AckReply.Ack)
      case None =>
        organizationProjectionRepository
          .store(
            OrganizationProjection(
              e.entityId,
              e.name,
              e.members,
              e.createdAt,
              e.createdBy,
              message.entityVersion,
              Metadata.empty
            )
          )
          .map(_ => AckReply.Ack)
    }

  def handle(
      e: MemberAddedToOrganization,
      message: EventStoreRecord[OrganizationId, OrganizationEvent]
  ): Future[AckReply] =
    organizationProjectionRepository.find(e.entityId).flatMap {
      case None =>
        logger
          .warn(
            s"No organization ${e.entityId} found thought it should already be created. $e"
          )
        Future.successful(AckReply.Nack)
      case Some(organization) =>
        if (message.entityVersion <= organization.latestOrganizationEntityVersion) {
          logger.warn(
            s"Event already handled. Ignoring. $organization, event: $message"
          )
          Future.successful(AckReply.Ack)
        } else {
          val updatedOrganization =
            organization.copy(
              members = organization.members :+ e.member,
              latestOrganizationEntityVersion = message.entityVersion
            )
          organizationProjectionRepository
            .store(updatedOrganization)
            .map(_ => AckReply.Ack)
        }
    }

}
