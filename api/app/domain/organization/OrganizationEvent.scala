package domain.organization

import tk.lib.core.auth.api.UserId
import tk.lib.eventsourcing.Event

import java.time.Instant

sealed trait OrganizationEvent extends Event[OrganizationId]

case class OrganizationCreated(
    entityId: OrganizationId,
    name: String,
    members: Seq[OrganizationMember],
    createdAt: Instant,
    createdBy: UserId
) extends OrganizationEvent

case class MemberAddedToOrganization(
    entityId: OrganizationId,
    member: OrganizationMember
) extends OrganizationEvent

case class RoleAddedToOrganizationMember(
    entityId: OrganizationId,
    member: OrganizationMember
) extends OrganizationEvent
