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

import com.google.cloud.pubsub.v1.{ SubscriptionAdminClient, TopicAdminClient }
import com.google.pubsub.v1.{
  ExpirationPolicy,
  ProjectSubscriptionName,
  Subscription,
  TopicName
}
import play.api.Logging
import tk.lib.core.concurrent.BlockingIoExecutionContext
import tk.lib.core.google.GoogleGeneralConfig

import scala.concurrent.{ ExecutionContext, Future }

class PubsubAdmin(
    googleGeneralConfig: GoogleGeneralConfig,
    blockingIoExecutionContext: BlockingIoExecutionContext
)(implicit ec: ExecutionContext)
    extends Logging {

  /**
    * Create topic.
    */
  def createTopic(topic: String): Future[Boolean] = {
    val topicName = TopicName.of(googleGeneralConfig.projectId, topic)
    logger.info(s"Creating topic ${topicName.toString}...")
    Future {
      val topicAdminClient = TopicAdminClient.create()
      topicAdminClient.createTopic(topicName)
      true
    }(blockingIoExecutionContext.underlying).recover {
      case e if e.getMessage.contains("ALREADY_EXISTS") =>
        logger.warn(s"Topic ${topicName.toString} already exists, ignoring.")
        false
    }
  }

  /**
    * Create subscription with message ordering.
    * Subscription will never expire.
    */
  def createSubscription(topic: String, subscription: String): Future[Boolean] = {
    val topicName = TopicName.of(googleGeneralConfig.projectId, topic)
    val subscriptionName =
      ProjectSubscriptionName.of(googleGeneralConfig.projectId, subscription)
    logger.info(
      s"Creating subscription ${subscriptionName.toString} for topic ${topicName.toString}..."
    )
    Future {
      val subscriptionAdminClient = SubscriptionAdminClient.create()

      // Never expire the subscription
      val expirationPolicy = ExpirationPolicy
        .newBuilder()
        .build()

      subscriptionAdminClient.createSubscription(
        Subscription
          .newBuilder()
          .setTopic(topicName.toString)
          .setName(subscriptionName.toString)
          .setEnableMessageOrdering(true)
          .setExpirationPolicy(expirationPolicy)
          .build()
      )
      true
    }(blockingIoExecutionContext.underlying).recover {
      case e if e.getMessage.contains("ALREADY_EXISTS") =>
        logger.warn(
          s"Subscription ${subscriptionName.toString} for topic  ${topicName.toString} already exists, ignoring."
        )
        false
    }
  }
}
