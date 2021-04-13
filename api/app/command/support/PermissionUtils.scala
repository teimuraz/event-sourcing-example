package command.support

import tk.lib.core.error.ForbiddenException
import play.api.Logging
import query.user.UserProjection
import java.util.UUID

import tk.lib.core.auth.api.AuthenticatedContext
import scala.concurrent.{ ExecutionContext, Future }

class PermissionUtils(userUtils: UserUtils)(implicit ec: ExecutionContext)
    extends Logging {

  def processPermissionCheck[R](
      req: R
  )(
      f: UserProjection => (Boolean, String)
  )(implicit context: AuthenticatedContext): Future[UserProjection] =
    for {
      user <- userUtils.getUserProjection(context.identity.userId)
      (allowed, message) = f(user)
      _ <- {
        if (!allowed) {
          processFailedCheck(message, user, req)
        } else {
          Future.successful(true)
        }
      }
    } yield {
      user
    }

//  def processPermissionCheck[R](
//      req: R
//  )(
//      f: UserProjection => Boolean
//  )(implicit context: AuthenticatedContext): Future[UserProjection] =
//    processPermissionCheckWithCustomMessage(req) { user =>
//      val allowed = f(user)
//      (allowed, "You don't have permission to perform this action")
//    }

  private[support] def processFailedCheck[R](
      logMessage: String,
      user: UserProjection,
      req: R
  ): Future[Boolean] = {
    val incidentId         = UUID.randomUUID()
    val logMessageEnriched = s"$logMessage | User $user | Request: $req"
    logger.warn(
      s"Permission check incident | Incident id $incidentId |$logMessageEnriched"
    )
    Future.failed(
      new ForbiddenException(
        s"You don't have rights to perform this action. Incident id: $incidentId"
      )
    )
  }

}
