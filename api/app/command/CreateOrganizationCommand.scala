package command

import tk.lib.core.auth.api.AuthenticatedContext
import tk.lib.core.datetime.DateTimeUtils
import tk.lib.core.error.ValidationException
import domain.organization.{
  OrganizationBehaviour,
  OrganizationRepository,
  OrganizationState
}
import query.user.UserProjection

import scala.concurrent.{ ExecutionContext, Future }
import CreateOrganizationCommand.CreateOrganizationRequest
import command.support.PermissionUtils
import play.api.libs.json.{ Json, OFormat }
import query.organization.OrganizationProjectionRepository
import jsonformat.DefaultFormats._

class CreateOrganizationCommand(
    organizationRepository: OrganizationRepository,
    organizationProjectionRepository: OrganizationProjectionRepository,
    permissionUtils: PermissionUtils,
    dateTimeUtils: DateTimeUtils
)(implicit val ec: ExecutionContext) {

  def execute(
      req: CreateOrganizationRequest
  )(implicit context: AuthenticatedContext): Future[OrganizationState] =
    for {
      _ <- checkPermission(req)
      _ <- organizationProjectionRepository.findByName(req.name).map {
        case Some(_) =>
          Future.failed(
            new ValidationException(s"Organization name ${req.name} is already taken")
          )
        case None => Future.successful(true)
      }
      organizationId <- organizationRepository.nextId()
      organization = OrganizationBehaviour.create(
        organizationId,
        req.name,
        Nil,
        dateTimeUtils.now,
        context.identity.userId
      )
      _ <- organizationRepository.store(organization)
    } yield {
      organization.state
    }

  def checkPermission(
      req: CreateOrganizationRequest
  )(implicit context: AuthenticatedContext): Future[UserProjection] =
    permissionUtils.processPermissionCheck(req) { user =>
      val allowed = user.systemManager.isDefined

      (allowed, s"User tried to create organization")
    }
}

object CreateOrganizationCommand {
  case class CreateOrganizationRequest(name: String)

  object CreateOrganizationRequest {
    implicit lazy val format: OFormat[CreateOrganizationRequest] =
      Json.format[CreateOrganizationRequest]
  }
}
