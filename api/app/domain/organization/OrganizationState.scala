package domain.organization

import tk.lib.core.auth.api.UserId

import java.time.Instant

case class OrganizationState(
    id: OrganizationId,
    name: String,
    members: Seq[OrganizationMember],
    createdAt: Instant,
    createdBy: UserId
) extends OrganizationBehaviour.State {

  override def applyEvent(event: OrganizationEvent): OrganizationState = event match {
    case e: OrganizationCreated =>
      copy(
        id = e.entityId,
        name = e.name,
        members = e.members,
        createdAt = e.createdAt,
        createdBy = e.createdBy
      )
    case e: MemberAddedToOrganization => copy(members = members :+ e.member)
    case e: RoleAddedToOrganizationMember =>
      copy(members = members.filter(_.userId != e.member.userId) :+ e.member)

  }
}
