package jsonformat

import domain.systemmanager.{ SystemManagerEvent, SystemManagerRole, SystemManagerState }
import julienrf.json.derived
import play.api.libs.json.{ __, Format, JsResult, JsString, JsValue, Json, Reads, Writes }
import tk.lib.core.json.syntax._

trait SystemManagerFormats extends TargetBasedFormats {
  /// SystemManagerState

  implicit lazy val systemManagerStateFormat: Format[SystemManagerState] =
    Json.format[SystemManagerState]

  /// SystemManagerEvent

  implicit lazy val systemManagerEventWrites: Writes[SystemManagerEvent] =
    derived.flat.owrites((__ \ "type").write[String])
  implicit lazy val systemManagerEventReads: Reads[SystemManagerEvent] =
    derived.flat.reads((__ \ "type").read[String])
  implicit lazy val systemManagerEventFormat: Format[SystemManagerEvent] =
    Format(systemManagerEventReads, systemManagerEventWrites)

  /// SystemManagerRole

  implicit lazy val systemManagerRoleFormat: Format[SystemManagerRole] =
    new Format[SystemManagerRole] {
      override def reads(json: JsValue): JsResult[SystemManagerRole] =
        json.validate[String].flatMap(SystemManagerRole(_))
      override def writes(o: SystemManagerRole): JsValue =
        JsString(o.stringValue)
    }

}
