package domain.user

import tk.lib.core.auth.api.UserId

import java.time.Instant

case class UserState(
    id: UserId,
    email: String,
    firstName: Option[String],
    lastName: Option[String],
    oauthUserId: String,
    createdAt: Instant
) extends UserBehaviour.State {
  override def applyEvent(event: UserEvent): UserState = event match {
    case e: UserCreated =>
      copy(
        id = e.entityId,
        email = e.email,
        firstName = e.firstName,
        lastName = e.lastName,
        oauthUserId = e.oauthUserId,
        createdAt = e.createdAt
      )
    case e: EmailChanged => copy(email = e.email)
  }
}
