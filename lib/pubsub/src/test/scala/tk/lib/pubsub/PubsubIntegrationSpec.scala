/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.pubsub

import UserEvent.UserCreated
import akka.actor.ActorSystem
import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.core.testutil.SpecLike
import tk.lib.pubsub.AckReply.Ack
import tk.lib.pubsub.Producer.MessageOrdering
import org.scalatest.freespec.AnyFreeSpec
import play.api.Logging

import scala.concurrent.{ ExecutionContext, Future }

/**
  * TODO:: run against pubsub emulator.
  */
class PubsubIntegrationSpec extends AnyFreeSpec with SpecLike with Logging {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher

  implicit val messageOrdering: MessageOrdering[UserCreated] =
    (message: UserCreated) => message.entityId.toString

  "publish and consume" in {
    val f = fixtures

    f.userEventConsumer.run()
    Thread.sleep(1000)

    val event: UserEvent = UserCreated(123, "john", "john@mail.com", "supersecret")
    logger.info(s"Publishing event $event")
    awaitResult(f.producer.publish(event, "testUserEventsTopics"))

    logger.info(s"Published event $event")

    // Wait a bit until consumer gets message
    Thread.sleep(5000)

    logger.info(f.handledMessage.get.toString)
    f.handledMessage should be(
      Some(UserCreated(123, "john", "john@mail.com", "supersecret"))
    )
  }

  def fixtures = new {

    val googleConfig = GoogleGeneralConfig("test-project")
    val producer     = new Producer(googleConfig)

    var handledMessage: Option[UserEvent] = None

    val messageHandler: UserEvent => Future[AckReply] = { message =>
      handledMessage = Some(message)
      Future.successful(Ack)
    }

    val userEventConsumer = new UserEventConsumer(
      messageHandler,
      googleConfig
    )
  }
}
