/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing

import tk.lib.pubsub.Producer
import scala.concurrent.ExecutionContext.Implicits.global

trait PubsubConnectorSpecLike extends IntegrationSpecLike {

  import tk.lib.eventsourcing.EventStore.EventStoreRecordDefaultFormat._

  lazy val producer =
    new Producer(googleConfig)

  lazy val pubsubConnectorOffsetManger =
    new PubsubConnectorOffsetManger(database)

  lazy val eventStorePubsubConnectorComponents: EventStorePubsubConnector.Components =
    EventStorePubsubConnector.Components(
      producer,
      actorSystem,
      eventStorePubsubConnectorConfig,
      pubsubConnectorOffsetManger
    )

  lazy val userEventStorePubsubConnector =
    new EventStorePubsubConnector[UserId, UserEvent](
      "user-event-store-connector",
      eventStore,
      eventStorePubsubConnectorComponents
    )

  lazy val eventStorePubsubConnectorsRunner: EventStorePubsubConnectorsRunner =
    EventStorePubsubConnectorsRunner(
      actorSystem,
      eventStorePubsubConnectorConfig,
      Seq(userEventStorePubsubConnector)
    )
}
