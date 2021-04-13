/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.pubsub

import tk.lib.pubsub.Producer.MessageOrdering
import play.api.libs.json.{ Format, JsError, JsValue, Json, Reads, Writes }

sealed trait UserEvent {
  def entityId: Long
}

object UserEvent {
  final case class UserCreated(
      entityId: Long,
      username: String,
      email: String,
      password: String
  ) extends UserEvent

  object UserCreated {
    implicit val writes = Json.writes[UserCreated]
    implicit val reads  = Json.reads[UserCreated]
  }

  final case class UsernameChanged(entityId: Long, username: String) extends UserEvent

  object UsernameChanged {
    implicit val writes = Json.writes[UsernameChanged]
    implicit val reads  = Json.reads[UsernameChanged]
  }

  implicit val reads: Reads[UserEvent] = (json: JsValue) => {
    for {
      eventType <- (json \ "eventType").validate[String]
      event <- eventType match {
        case "UserCreated" =>
          (json \ "payload").validate[UserCreated](UserCreated.reads)
        case "UsernameChanged" =>
          (json \ "payload")
            .validate[UsernameChanged](UsernameChanged.reads)
        case unknown => JsError(s"Unknown event type $unknown")
      }
    } yield {
      event
    }
  }

  implicit val writes: Writes[UserEvent] = {
    case e: UserCreated =>
      Json.obj(
        "eventType" -> "UserCreated",
        "payload"   -> Json.toJson(e)(UserCreated.writes)
      )
    case e: UsernameChanged =>
      Json.obj(
        "eventType" -> "UsernameChanged",
        "payload"   -> Json.toJson(e)(UsernameChanged.writes)
      )
  }

  implicit lazy val format: Format[UserEvent] = Format(reads, writes)

  implicit lazy val messageOrdering: MessageOrdering[UserEvent] = (message: UserEvent) =>
    message.entityId.toString
}
