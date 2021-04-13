package query.organization

import tk.lib.core.model.{ EntityLike, Metadata }
import domain.organization.OrganizationId
import java.time.Instant
import domain.organization.OrganizationMember
import tk.lib.core.auth.api.UserId

case class OrganizationProjection(
    id: OrganizationId,
    name: String,
    members: Seq[OrganizationMember],
    createdAt: Instant,
    createdBy: UserId,
    latestOrganizationEntityVersion: Int,
    metadata: Metadata
)

object OrganizationProjection {
  implicit val entityLike: EntityLike[OrganizationProjection, OrganizationId] =
    EntityLike.auto
}
