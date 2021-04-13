package command.support

import tk.lib.core.error.ValidationException
import query.organization.{ OrganizationProjection, OrganizationProjectionRepository }

import scala.concurrent.{ ExecutionContext, Future }
import domain.organization.{
  OrganizationBehaviour,
  OrganizationId,
  OrganizationRepository
}

class OrganizationUtils(
    organizationRepository: OrganizationRepository,
    organizationProjectionRepository: OrganizationProjectionRepository
)(implicit ec: ExecutionContext) {

  def getOrganization(
      organizationId: OrganizationId
  ): Future[OrganizationBehaviour.Entity] =
    organizationRepository
      .find(organizationId)
      .map(
        _.getOrElse(
          throw new ValidationException(
            s"No organization with id $organizationId found "
          )
        )
      )

  def getOrganizationProjection(
      organizationId: OrganizationId
  ): Future[OrganizationProjection] =
    organizationProjectionRepository
      .find(organizationId)
      .map(
        _.getOrElse(
          throw new ValidationException(
            s"No organization with id $organizationId found "
          )
        )
      )

}
