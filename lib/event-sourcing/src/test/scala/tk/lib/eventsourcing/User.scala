/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing

import tk.lib.eventsourcing.UserEvent.{ UserActivated, UserCreated, UsernameChanged }
import julienrf.json.derived
import play.api.libs.json.{
  __,
  Format,
  JsError,
  JsNumber,
  JsResult,
  JsSuccess,
  JsValue,
  Json,
  Reads,
  Writes
}
import reactivemongo.api.bson.{ BSONHandler, Macros }

/**
  * User entity behaviour
  */
object UserBehaviour extends EntityBehaviour {

  def create(id: UserId, username: String, email: String, status: UserStatus): Entity =
    emptyEntity.applyChange(UserCreated(id, username, email, status))

  def activate(entity: Entity): Entity = withState(entity) { state =>
    entity.applyChange(UserActivated(state.id, UserStatus.Active))
  }

  def changeUsername(newUsername: String)(entity: Entity): Entity = withState(entity) {
    state =>
      entity.applyChange(UsernameChanged(state.id, newUsername))
  }

  override type Id  = UserId
  override type Evt = UserEvent
  override type S   = UserState

  override val emptyState: UserState =
    UserState(UserId(0), "", "", UserStatus.PendingActivation)
}

case class UserId(value: Long) extends AnyVal

object UserId {
  implicit lazy val format = Json.valueFormat[UserId]
  implicit val handler: BSONHandler[UserId] =
    Macros.valueHandler[UserId]

}

/**
  * User state
  */
case class UserState(
    id: UserId,
    username: String,
    email: String,
    status: UserStatus
) extends UserBehaviour.State {
  override def applyEvent(event: UserEvent): UserState = event match {
    case e: UserCreated     => copy(e.entityId, e.username, e.email, e.status)
    case e: UserActivated   => copy(status = e.status)
    case e: UsernameChanged => copy(username = e.username)
  }
}

object UserState {
  implicit val format: Format[UserState] = Json.format[UserState]
}

/**
  * User status
  */
sealed trait UserStatus

object UserStatus {
  case object PendingActivation extends UserStatus
  case object Active            extends UserStatus

  def intValueOf(status: UserStatus): Int =
    status match {
      case PendingActivation => 0
      case Active            => 1
    }

  def valueOf(value: Int): Either[String, UserStatus] =
    value match {
      case 0       => Right(PendingActivation)
      case 1       => Right(Active)
      case unknown => Left(s"Unknown status $unknown")
    }

  implicit val format: Format[UserStatus] = new Format[UserStatus] {
    override def reads(json: JsValue): JsResult[UserStatus] =
      json.validate[Int].flatMap { value =>
        UserStatus.valueOf(value) match {
          case Right(status) => JsSuccess(status)
          case Left(error)   => JsError(error)
        }
      }
    override def writes(o: UserStatus): JsValue =
      JsNumber(UserStatus.intValueOf(o))
  }
}

/**
  * User event
  */
sealed trait UserEvent extends Event[UserId]

object UserEvent {

  case class UserCreated(
      entityId: UserId,
      username: String,
      email: String,
      status: UserStatus
  ) extends UserEvent

  case class UserActivated(entityId: UserId, status: UserStatus) extends UserEvent

  case class UsernameChanged(entityId: UserId, username: String) extends UserEvent

  implicit lazy val writes: Writes[UserEvent] =
    derived.flat.owrites((__ \ "type").write[String])
  implicit lazy val reads: Reads[UserEvent] =
    derived.flat.reads((__ \ "type").read[String])
  implicit lazy val format: Format[UserEvent] = Format(reads, writes)

  implicit val bsonHandler: BSONHandler[UserEvent] =
    Macros.handler[UserEvent]
}
