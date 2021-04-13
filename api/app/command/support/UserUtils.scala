package command.support

import tk.lib.core.auth.api.{ AuthenticatedContext, UserId }
import tk.lib.core.error.ValidationException
import query.user.{ UserProjection, UserProjectionRepository }

import scala.concurrent.{ ExecutionContext, Future }

class UserUtils(userProjectionRepository: UserProjectionRepository)(
    implicit ec: ExecutionContext
) {

  def getUserProjection(
      userId: UserId,
      message: Option[String] = None
  ): Future[UserProjection] =
    userProjectionRepository
      .find(userId)
      .map(
        _.getOrElse(
          throw new ValidationException(
            message.getOrElse(s"No user with id $userId found")
          )
        )
      )

  def assertUserExistence(userId: UserId): Future[UserProjection] =
    getUserProjection(userId)

  def getCurrentUserProjection()(
      implicit authenticatedContext: AuthenticatedContext
  ): Future[UserProjection] = getUserProjection(authenticatedContext.identity.userId)

}
