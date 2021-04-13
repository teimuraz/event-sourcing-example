/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.pubsub

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import tk.lib.core.datetime.DateTimeUtils
import tk.lib.core.google.GoogleGeneralConfig
import play.api.libs.json.Reads

import scala.concurrent.{ ExecutionContext, Future }

class UserEventConsumer(
    messageHandler: UserEvent => Future[AckReply],
    val googleConfig: GoogleGeneralConfig
)(
    implicit val ec: ExecutionContext,
    val actorSystem: ActorSystem
) extends Consumer[UserEvent] {

  override def subscription: String = "testUserEventsTopics_userProjectionsSubscription"

  override implicit val messageReads: Reads[UserEvent] = UserEvent.reads

  override def handle(message: UserEvent): Future[AckReply] =
    messageHandler(message)
}
