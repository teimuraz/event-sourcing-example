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

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import tk.lib.core.google.GoogleGeneralConfig
import play.api.libs.json.{ Json, Writes }

import scala.concurrent.{ ExecutionContext, Future }
import akka.stream.scaladsl._
import akka.util.Timeout
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{ PubsubMessage, TopicName }

import scala.concurrent.duration.DurationInt
import tk.lib.core.google.ApiFutureOps
import tk.lib.pubsub.Producer.{ MessageOrdering, PublisherRepositoryActor }
import tk.lib.pubsub.Producer.PublisherRepositoryActor.{ Cleanup, GetPublisher }
import akka.pattern.ask

import java.util.concurrent.TimeUnit
import tk.lib.core.futureutils.runSequentially

/**
  * Google Pubsub producer.
  */
class Producer(googleGeneralConfig: GoogleGeneralConfig)(
    implicit ec: ExecutionContext,
    actorSystem: ActorSystem
) {

  private val publisherRepositoryActor = actorSystem.actorOf(
    PublisherRepositoryActor.props(googleGeneralConfig)
  )

  implicit private val timeout: Timeout = 5.seconds

  def publish[M: Writes: MessageOrdering](
      message: M,
      topic: String
  ): Future[String] = {

    val pubsubMessage = preparePubsubMessage(message)

    for {
      publisher <- (publisherRepositoryActor ? GetPublisher(topic))
        .map(p => p.asInstanceOf[Publisher])
      result <- publisher.publish(pubsubMessage).toScala
    } yield {
      result
    }
  }

  def publish[M: Writes: MessageOrdering](
      messages: Seq[M],
      topic: String
  ): Future[Seq[String]] = {
    // TODO:: Optimize it
    val publishFutures = messages.map { message => () =>
      publish(message, topic)
    }
    runSequentially(publishFutures)
  }

  def cleanup(): Future[Any] =
    publisherRepositoryActor ? Cleanup

  private def preparePubsubMessage[M: Writes: MessageOrdering](
      message: M
  ): PubsubMessage = {
    val json         = Json.toJson(message)
    val jsonAsString = json.toString()
    val orderingKey  = implicitly[MessageOrdering[M]].orderingKey(message)

    PubsubMessage
      .newBuilder()
      .setOrderingKey(orderingKey)
      .setData(ByteString.copyFromUtf8(jsonAsString))
      .build()
  }
}

object Producer {

  trait MessageOrdering[M] {
    def orderingKey(message: M): String
  }

  class PublisherRepositoryActor(
      googleGeneralConfig: GoogleGeneralConfig
  ) extends Actor
      with ActorLogging {

    private var publishers: Map[String, Publisher] = Map.empty

    override def receive: Receive = {
      case GetPublisher(topic) =>
        val publisher = publishers.get(topic) match {
          case None =>
            val publisher = createPublisher(topic)
            publishers = publishers.+((topic, publisher))
            publisher
          case Some(publisher) => publisher
        }
        sender() ! publisher
      case Cleanup =>
        publishers.values.foreach { publisher =>
          publisher.shutdown()
          publisher.awaitTermination(1, TimeUnit.MINUTES)
        }

    }

    private def createPublisher(topic: String): Publisher = {
      val topicName = TopicName.of(googleGeneralConfig.projectId, topic)
      Publisher.newBuilder(topicName).setEnableMessageOrdering(true).build()
    }
  }

  object PublisherRepositoryActor {

    case class GetPublisher(topic: String)

    case object Cleanup

    def props(googleGeneralConfig: GoogleGeneralConfig) =
      Props(classOf[PublisherRepositoryActor], googleGeneralConfig)
  }
}
