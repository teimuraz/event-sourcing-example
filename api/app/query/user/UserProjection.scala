package query.user

import tk.lib.core.auth.api.UserId
import domain.organization.OrganizationMember
import domain.systemmanager.SystemManagerState
import play.api.libs.json.{ Json, OFormat }

import java.time.Instant
import tk.lib.core.model.{ EntityLike, Metadata }

case class UserProjection(
    id: UserId,
    email: String,
    firstName: Option[String],
    lastName: Option[String],
    oauthUserId: String,
    createdAt: Instant,
    latestUserEntityVersion: Int,
    systemManager: Option[SystemManagerState],
    latestSystemManagerEntityVersion: Int,
    metadata: Metadata
)

object UserProjection {

  implicit val entityLike: EntityLike[UserProjection, UserId] =
    EntityLike.auto

  case class OrganizationMembership(
      member: OrganizationMember,
      latestOrganizationEntityVersion: Int
  )
}
