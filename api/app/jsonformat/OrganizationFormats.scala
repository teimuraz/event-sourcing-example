package jsonformat

import play.api.libs.json.{
  __,
  Format,
  JsResult,
  JsString,
  JsValue,
  Json,
  OFormat,
  Reads,
  Writes
}
import tk.lib.core.json.syntax._
import domain.organization.OrganizationMember.Role
import domain.organization.{
  OrganizationEvent,
  OrganizationId,
  OrganizationMember,
  OrganizationState
}
import julienrf.json.derived
import query.organization.OrganizationProjection

trait OrganizationFormats extends TargetBasedFormats {

  /// OrganizationId

  implicit lazy val organizationIdFormat: Format[OrganizationId] =
    Json.valueFormat[OrganizationId]

  /// OrganizationState

  implicit lazy val organizationStateFormat: Format[OrganizationState] =
    Json.format[OrganizationState]

  /// OrganizationEvent

  implicit lazy val organizationEventWrites: Writes[OrganizationEvent] =
    derived.flat.owrites((__ \ "type").write[String])
  implicit lazy val organizationEventReads: Reads[OrganizationEvent] =
    derived.flat.reads((__ \ "type").read[String])
  implicit lazy val organizationEventFormat: Format[OrganizationEvent] =
    Format(organizationEventReads, organizationEventWrites)

  /// Member

  implicit lazy val organizationMemberFormat: OFormat[OrganizationMember] =
    Json.format[OrganizationMember]

  /// Member.Role

  implicit lazy val organizationMemberRoleFormat: Format[Role] = new Format[Role] {
    override def reads(json: JsValue): JsResult[Role] =
      json.validate[String].flatMap(OrganizationMember.Role(_))
    override def writes(o: Role): JsValue =
      JsString(o.stringValue)
  }

  /// OrganizationProjection

  implicit lazy val organizationProjectionFormat: OFormat[OrganizationProjection] =
    Json.format[OrganizationProjection]

}
