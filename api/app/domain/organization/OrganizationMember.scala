package domain.organization

import tk.lib.core.auth.api.UserId
import tk.lib.core.error.ValidationException
import domain.organization.OrganizationMember.Role
import domain.organization.OrganizationMember.Role.{ Admin, Employee }

import scala.util.{ Failure, Success, Try }

case class OrganizationMember(userId: UserId, roles: Seq[Role]) {
  def addRole(
      role: Role,
      organizationId: OrganizationId
  ): Try[RoleAddedToOrganizationMember] =
    if (roles.contains(role)) {
      Failure(new ValidationException(s"User $userId already has $role role"))
    } else {
      Success(RoleAddedToOrganizationMember(organizationId, copy(roles = roles :+ role)))
    }
}

object OrganizationMember {
  sealed trait Role {
    def stringValue: String = this match {
      case Admin    => "ADMIN"
      case Employee => "EMPLOYEE"
    }
  }

  object Role {
    case object Admin    extends Role
    case object Employee extends Role

    def apply(value: String): Either[String, Role] = value.trim.toUpperCase match {
      case "ADMIN"    => Right(OrganizationMember.Role.Admin)
      case "EMPLOYEE" => Right(OrganizationMember.Role.Employee)
      case unknown    => Left(s"Unknown role $unknown")
    }

    def stringValueOf(role: Role): String =
      role match {
        case Admin    => "ADMIN"
        case Employee => "EMPLOYEE"
      }
  }
}
