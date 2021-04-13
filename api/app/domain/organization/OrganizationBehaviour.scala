package domain.organization

import java.time.Instant
import tk.lib.core.auth.api.UserId
import tk.lib.core.error.ValidationException
import tk.lib.eventsourcing.EntityBehaviour
import domain.organization.OrganizationMember.Role
import scala.util.{ Failure, Success, Try }

object OrganizationBehaviour extends EntityBehaviour {

  def create(
      id: OrganizationId,
      name: String,
      members: Seq[OrganizationMember],
      createdAt: Instant,
      createdBy: UserId
  ): Entity =
    emptyEntity.applyChange(OrganizationCreated(id, name, members, createdAt, createdBy))

  def addMember(
      organization: Entity,
      member: OrganizationMember
  ): Try[Entity] = withState(organization) { state =>
    state.members.find(_.userId == member.userId) match {
      case Some(_) =>
        Failure(
          new ValidationException(
            s"Member ${member.userId} is already exist in organization ${state.id}"
          )
        )
      case None =>
        val normalizedMember = member.copy(roles = member.roles.distinct)
        Success(
          organization.applyChange(MemberAddedToOrganization(state.id, normalizedMember))
        )
    }
  }

  def addRoleToMember(organization: Entity, userId: UserId, role: Role): Try[Entity] =
    withState(organization) { state =>
      state.members.find(_.userId == userId) match {
        case None =>
          Failure(
            new ValidationException(
              s"Member $userId is not a part of organization ${state.id}"
            )
          )
        case Some(member) =>
          member.addRole(role, state.id).map(e => organization.applyChange(e))
      }
    }

  def isUserOrganizationAdmin(organization: Entity, userId: UserId): Boolean =
    withState(organization) { state =>
      state.members.find(_.userId == userId).exists(_.roles.contains(Role.Admin))
    }

  override type S   = OrganizationState
  override type Id  = OrganizationId
  override type Evt = OrganizationEvent

  override val emptyState: OrganizationState =
    OrganizationState(OrganizationId(0), "", Nil, Instant.now(), UserId(0))

}

case class OrganizationId(value: Long) extends AnyVal
