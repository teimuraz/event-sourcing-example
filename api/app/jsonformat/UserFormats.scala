package jsonformat

import tk.lib.core.auth.api.UserId
import domain.user.{ UserEvent, UserState }
import julienrf.json.derived
import play.api.libs.json.{ __, Format, Json, OFormat, Reads, Writes }
import query.user.UserProjection
import query.user.UserProjection.OrganizationMembership

trait UserFormats
    extends SystemManagerFormats
    with OrganizationFormats
    with TargetBasedFormats {

  /// UserId

  implicit lazy val userIdFormat: Format[UserId] = Json.valueFormat[UserId]

  /// UserState

  implicit lazy val userStateFormat: Format[UserState] = Json.format

  /// UserEvent

  implicit lazy val userEventWrites: Writes[UserEvent] =
    derived.flat.owrites((__ \ "type").write[String])
  implicit lazy val userEventReads: Reads[UserEvent] =
    derived.flat.reads((__ \ "type").read[String])
  implicit lazy val userEventFormat: Format[UserEvent] =
    Format(userEventReads, userEventWrites)

  /// UserProjection

  implicit lazy val userProjectionFormat: OFormat[UserProjection] =
    Json.format[UserProjection]

  implicit lazy val organizationMembership: OFormat[OrganizationMembership] =
    Json.format[OrganizationMembership]
}
