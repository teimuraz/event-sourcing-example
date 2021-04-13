package module

import com.google.pubsub.v1.{ Subscription, Topic }
import config.GoogleConfig.PubsubConfig
import tk.lib.pubsub.PubsubAdmin

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Create pubsub topics, subscriptions, etc.
  * TODO:: create topics and subscriptions automatically (if needed) when topic / subscription is invoked first time.
  */
class GoogleResourcesCreator(pubsubAdmin: PubsubAdmin, pubsubConfig: PubsubConfig)(
    implicit ec: ExecutionContext
) {

  def create(): Future[Boolean] =
    for {
      _ <- createTopics()
      _ <- createSubscriptions()
    } yield {
      true
    }

  def createTopics(): Future[Seq[Boolean]] =
    Future.sequence(
      Seq(
        pubsubConfig.userEventsTopic,
        pubsubConfig.systemManagerEventsTopic,
        pubsubConfig.organizationEventsTopic
      ).map(topic => pubsubAdmin.createTopic(topic))
    )

  def createSubscriptions(): Future[Seq[Boolean]] =
    Future.sequence(
      Seq(
        (
          pubsubConfig.userEventsTopic,
          pubsubConfig.userEventsTopic_userProjectionsSubscription
        ),
        (
          pubsubConfig.systemManagerEventsTopic,
          pubsubConfig.systemManagerEventsTopic_userProjectionsSubscription
        ),
        (
          pubsubConfig.organizationEventsTopic,
          pubsubConfig.organizationEventsTopic_organizationProjectionsSubscription
        )
      ).map {
        case (topic, subscription) =>
          pubsubAdmin.createSubscription(topic, subscription)
      }
    )

}
