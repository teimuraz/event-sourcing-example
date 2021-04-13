package domain.user

import tk.lib.core.auth.api.UserId
import tk.lib.eventsourcing.Event

import java.time.Instant

sealed trait UserEvent extends Event[UserId]

case class UserCreated(
    entityId: UserId,
    email: String,
    firstName: Option[String],
    lastName: Option[String],
    oauthUserId: String,
    createdAt: Instant
) extends UserEvent

case class EmailChanged(entityId: UserId, email: String) extends UserEvent
