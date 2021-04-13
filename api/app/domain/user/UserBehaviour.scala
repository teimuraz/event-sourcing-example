package domain.user

import java.time.Instant
import tk.lib.core.auth.api.UserId
import tk.lib.eventsourcing.EntityBehaviour

object UserBehaviour extends EntityBehaviour {
  def create(
      id: UserId,
      email: String,
      firstName: Option[String],
      lastName: Option[String],
      oauthUserId: String,
      createdAt: Instant
  ): UserBehaviour.Entity =
    emptyEntity.applyChange(
      UserCreated(id, email, firstName, lastName, oauthUserId, createdAt)
    )

  override type Id  = UserId
  override type Evt = UserEvent
  override type S   = UserState

  override val emptyState: UserState =
    UserState(UserId(0), "", None, None, "", Instant.now())
}
