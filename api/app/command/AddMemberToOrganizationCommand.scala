package command

import domain.organization.{
  OrganizationBehaviour,
  OrganizationId,
  OrganizationMember,
  OrganizationRepository,
  OrganizationState
}
import query.organization.OrganizationProjectionRepository
import query.user.{ UserProjection, UserProjectionRepository }

import scala.concurrent.{ ExecutionContext, Future }
import play.api.libs.json.{ Json, OFormat }
import tk.lib.core.auth.api.AuthenticatedContext
import command.AddMemberToOrganizationCommand.AddMemberToOrganizationRequest
import command.support.{ OrganizationUtils, PermissionUtils, UserUtils }
import jsonformat.DefaultFormats._

class AddMemberToOrganizationCommand(
    val organizationRepository: OrganizationRepository,
    val userProjectionRepository: UserProjectionRepository,
    val organizationProjectionRepository: OrganizationProjectionRepository,
    userUtils: UserUtils,
    organizationUtils: OrganizationUtils,
    permissionUtils: PermissionUtils
)(implicit val ec: ExecutionContext) {

  def execute(
      req: AddMemberToOrganizationRequest
  )(implicit context: AuthenticatedContext): Future[OrganizationState] =
    for {
      organization <- organizationUtils.getOrganization(req.organizationId)
      _            <- checkPermission(organization, req)
      _            <- userUtils.assertUserExistence(req.member.userId)
      updatedOrganization <- Future.fromTry(
        OrganizationBehaviour.addMember(organization, req.member)
      )
      _ <- organizationRepository.store(updatedOrganization)
    } yield {
      updatedOrganization.state
    }

  def checkPermission(
      organization: OrganizationBehaviour.Entity,
      req: AddMemberToOrganizationRequest
  )(implicit context: AuthenticatedContext): Future[UserProjection] =
    permissionUtils.processPermissionCheck(req) { user =>
      val isSystemManager = user.systemManager.isDefined
      val isOrganizationAdmin =
        OrganizationBehaviour.isUserOrganizationAdmin(organization, user.id)
      val allowed = isSystemManager || isOrganizationAdmin

      (allowed, s"User tried to add member to organization")
    }
}

object AddMemberToOrganizationCommand {
  case class AddMemberToOrganizationRequest(
      organizationId: OrganizationId,
      member: OrganizationMember
  )

  object AddMemberToOrganizationRequest {
    implicit val format: OFormat[AddMemberToOrganizationRequest] =
      Json.format[AddMemberToOrganizationRequest]
  }
}
