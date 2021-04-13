/*
 *
 * Copyright 2021 TK
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package tk.lib.pubsub

import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.pubsub.AckReply.Nack
import play.api.Logging
import play.api.libs.json.{ JsError, JsSuccess, JsValue, Json, Reads }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.cloud.pubsub.v1.Subscriber
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.pubsub.v1.PubsubMessage

/**
  * Google Pubsub consumer.
  */
trait Consumer[M] extends Logging {
  def subscription: String
  def googleConfig: GoogleGeneralConfig
  implicit def ec: ExecutionContext
  implicit val messageReads: Reads[M]

  def handle(message: M): Future[AckReply]

  private val subscriptionName: ProjectSubscriptionName =
    ProjectSubscriptionName.of(googleConfig.projectId, subscription)

  private val receiver: MessageReceiver =
    (message: PubsubMessage, consumer: AckReplyConsumer) => {
      handlePubsubMessage(message).map {
        case AckReply.Ack  => consumer.ack()
        case AckReply.Nack => consumer.nack()
      }
    }

  private val subscriber = Subscriber.newBuilder(subscriptionName, receiver).build

  def run() =
    Try(subscriber.startAsync())
      .recover {
        case e =>
          logger.error(
            s"Failed to start subscriber: $subscription, error: ${e.getMessage}",
            e
          )
      }
      .getOrElse(())

  def stop(): Future[Unit] = Future.successful(subscriber.awaitTerminated())

  private def handlePubsubMessage(pubsubMessage: PubsubMessage): Future[AckReply] = {
    val rawJson = pubsubMessage.getData.toStringUtf8

    val result = for {
      json           <- Future.fromTry(parseJson(rawJson))
      message        <- Future.fromTry(decodeMessage(json))
      acknowledgment <- handle(message)
    } yield {
      acknowledgment
    }

    result.recover {
      case e =>
        logger.error(
          s"Failed to handle message. Message: $pubsubMessage, error: ${e.getMessage}",
          e
        )
        Nack
    }
  }

  private def parseJson(message: String): Try[JsValue] =
    Try(Json.parse(message)).recoverWith {
      case e =>
        Failure(
          new PubsubException(
            s"Failed to parse message as json. Message: $message, Error: ${e.getMessage}",
            Some(e)
          )
        )
    }

  private def decodeMessage(message: JsValue): Try[M] =
    message.validate[M] match {
      case JsSuccess(value, _) => Success(value)
      case JsError(errors) =>
        Failure(
          new PubsubException(s"Failed to decode message $message, errors: $errors")
        )
    }
}
