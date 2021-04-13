package module

import com.softwaremill.macwire.wire
import command.{ AddMemberToOrganizationCommand, CreateOrganizationCommand }
import command.support.OrganizationUtils
import domain.organization.{ OrganizationBehaviour, OrganizationRepository }
import query.organization.{ OrganizationEventConsumer, OrganizationProjectionRepository }
import router.OrganizationRouter
import jsonformat.DefaultFormats._
import tk.lib.eventsourcing.EventStorePubsubConnector

trait OrganizationComponents extends EssentialComponents with UserComponents {
  lazy val organizationUtils: OrganizationUtils = wire[OrganizationUtils]

  lazy val organizationRepository: OrganizationRepository = wire[OrganizationRepository]

  lazy val organizationPubsubConnector =
    new EventStorePubsubConnector[OrganizationBehaviour.Id, OrganizationBehaviour.Evt](
      "organizationEvents",
      organizationRepository.eventStore,
      eventStorePubsubConnectorComponents
    )

  lazy val organizationProjectionRepository: OrganizationProjectionRepository =
    wire[OrganizationProjectionRepository]

  lazy val organizationEventConsumer: OrganizationEventConsumer =
    wire[OrganizationEventConsumer]

  /// Commands

  lazy val createOrganizationCommand: CreateOrganizationCommand =
    wire[CreateOrganizationCommand]

  lazy val addMemberToOrganizationCommand: AddMemberToOrganizationCommand =
    wire[AddMemberToOrganizationCommand]

  /// Router

  lazy val organizationRouter: OrganizationRouter = wire[OrganizationRouter]
}
