/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing

import tk.lib.eventsourcing.UserProjectionRepository.UserProjection
import org.scalatest.freespec.AnyFreeSpec

import scala.concurrent.ExecutionContext.Implicits.global
import UserBehaviour._
import tk.lib.core.functionutils.valueToFunc1
import tk.lib.eventsourcing.UserStatus.PendingActivation

class EventStorePubsubConnectorIntegrationSpec
    extends AnyFreeSpec
    with PubsubConnectorSpecLike
    with IntegrationSpecLike {

  "consumer events from event store via pubsub" in {

    // Run pubsub consumer, will create user projection from events from event store published to the pubsub topic
    val userEventConsumer =
      new UserEventConsumer(
        googleConfig,
        userProjectionRepository
      )
    userEventConsumer.run()

    // Run pubsub connector so events from event store will be published to the pubsub topic
    eventStorePubsubConnectorsRunner.run()

    // Write some events to event store
    val flow1 = valueToFunc1(
        create(UserId(1), "john", "john@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername("jonny")
    val user1 = flow1()

    val flow2 = valueToFunc1(
        create(UserId(2), "james", "james@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername("jamesey")
    val user2 = flow2()

    val flow3 = valueToFunc1(
        create(UserId(3), "bill", "bill@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername("billy")
    val user3 = flow3()

    val flow4 = valueToFunc1(
        create(UserId(4), "martin", "martin@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername("martini")
    val user4 = flow4()

    awaitResult(userRepository.store(user1))
    awaitResult(userRepository.store(user2))
    awaitResult(userRepository.store(user3))
    awaitResult(userRepository.store(user4))

    // Wait
    // TODO:: too long (if it is less than 10 seconds, test fails, fix it and implement test properly)
    Thread.sleep(2000)

    // At this moment userEventConsumer had to create user projections in the database
    val user1Projection =
      awaitResult(userProjectionRepository.find(UserId(1))).get
    user1Projection should be(
      UserProjection(
        UserId(1),
        "jonny",
        "john@mail.com",
        UserStatus.Active,
        tk.lib.core.model.Metadata(0)
      )
    )

    val user2Projection =
      awaitResult(userProjectionRepository.find(UserId(2))).get
    user2Projection should be(
      UserProjection(
        UserId(2),
        "jamesey",
        "james@mail.com",
        UserStatus.Active,
        tk.lib.core.model.Metadata(0)
      )
    )

    val user3Projection =
      awaitResult(userProjectionRepository.find(UserId(3))).get
    user3Projection should be(
      UserProjection(
        UserId(3),
        "billy",
        "bill@mail.com",
        UserStatus.Active,
        tk.lib.core.model.Metadata(0)
      )
    )

    val user4Projection =
      awaitResult(userProjectionRepository.find(UserId(4))).get
    user4Projection should be(
      UserProjection(
        UserId(4),
        "martini",
        "martin@mail.com",
        UserStatus.Active,
        tk.lib.core.model.Metadata(0)
      )
    )
  }
}
