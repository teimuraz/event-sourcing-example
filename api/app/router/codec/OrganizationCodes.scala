package router.codec

import command.AddMemberToOrganizationCommand.AddMemberToOrganizationRequest
import command.CreateOrganizationCommand.CreateOrganizationRequest
import domain.organization
import domain.organization.OrganizationMember
import domain.organization.OrganizationId
import domain.organization.OrganizationMember.Role
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.{ Codec, Schema }

trait OrganizationCodes extends UserCodecs {

  /// OrganizationId

  implicit lazy val organizationIdCodec: PlainCodec[OrganizationId] =
    Codec.long
      .map(v => OrganizationId(v))(_.value)
  implicit lazy val organizationIdSchema: Schema[OrganizationId] =
    organizationIdCodec.schema

  /// OrganizationState

  implicit lazy val organizationStateSchema: Schema[organization.OrganizationState] =
    Schema.derived

  /// Member

  implicit lazy val organizationMemberStateSchema: Schema[OrganizationMember] =
    Schema.derived

  /// Member.Role

  lazy val organizationMemberRoleAdminSchema: Schema[Role.Admin.type] =
    Schema.derived[OrganizationMember.Role.Admin.type]
  lazy val organizationMemberRoleEmployeeSchema: Schema[Role.Employee.type] =
    Schema.derived[OrganizationMember.Role.Employee.type]

  implicit val organizationMemberRoleSchema: Schema[OrganizationMember.Role] =
    Schema.oneOfUsingField[OrganizationMember.Role, String](
      v => v.stringValue,
      v => v
    )(
      "ADMIN"    -> organizationMemberRoleAdminSchema,
      "EMPLOYEE" -> organizationMemberRoleEmployeeSchema
    )

  implicit lazy val createOrganizationRequestSchema: Schema[CreateOrganizationRequest] =
    Schema.derived

  implicit lazy val addMemberToOrganizationRequestSchema
      : Schema[AddMemberToOrganizationRequest] = Schema.derived

}
