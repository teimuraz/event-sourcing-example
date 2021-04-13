package router.codec

import julienrf.json.derived
import play.api.libs.json.{ __, Format, JsResult, JsString, JsValue, Json, Reads, Writes }
import sttp.tapir.Schema
import scala.util.{ Success, Try }
import tk.lib.core.json.syntax._
import tk.lib.core.eitherutils.syntax._
import domain.systemmanager.SystemManagerRole.Admin
import domain.systemmanager.{
  SystemManagerCreated,
  SystemManagerEvent,
  SystemManagerRole,
  SystemManagerRoleAssigned,
  SystemManagerState
}

trait SystemManagerCodecs extends UserCodecs {

  /// SystemManagerState

  implicit lazy val systemManagerStateSchema: Schema[SystemManagerState] =
    Schema.derived

  /// SystemManagerRole

  lazy val systemManagerRoleAdminSchema: Schema[SystemManagerRole.Admin.type] =
    Schema.derived[SystemManagerRole.Admin.type]

  implicit val systemManagerRoleSchema: Schema[SystemManagerRole] =
    Schema.oneOfUsingField[SystemManagerRole, String](
      v => v.stringValue,
      v => v
    )(
      "ADMIN" -> systemManagerRoleAdminSchema
    )

}
