/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing

import tk.lib.eventsourcing.EventStore.NewEventStoreRecord
import tk.lib.eventsourcing.UserStatus.PendingActivation
import UserBehaviour._
import tk.lib.core.functionutils.valueToFunc1

class EventStoreIntegrationSpec extends IntegrationSpecLike {

  "getEventsOfEntity" in {
    UserBehaviour.create(UserId(189), "john", "john@mail.com", PendingActivation)
    val flow1 = valueToFunc1(
        create(UserId(189), "john", "john@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername(
        "jonny"
      )

    val user1 = flow1()

    val eventStoreRecords1 = Seq(
      NewEventStoreRecord(user1.state.id, 1, user1.metadata.uncommittedChanges.head),
      NewEventStoreRecord(user1.state.id, 2, user1.metadata.uncommittedChanges(1)),
      NewEventStoreRecord(user1.state.id, 3, user1.metadata.uncommittedChanges(2))
    )

    awaitResult(
      eventStore.storeEvents(
        eventStoreRecords1
      )
    )

    val flow2 = valueToFunc1(
        create(UserId(200), "bill", "bill@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername("billy")
    val user2 = flow2()

    val eventStoreRecords2 = Seq(
      NewEventStoreRecord(user2.state.id, 1, user2.metadata.uncommittedChanges.head),
      NewEventStoreRecord(user2.state.id, 2, user2.metadata.uncommittedChanges(1)),
      NewEventStoreRecord(user2.state.id, 3, user2.metadata.uncommittedChanges(2))
    )

    awaitResult(eventStore.storeEvents(eventStoreRecords2))

    val persistedEvents2 =
      awaitResult(eventStore.getEventsOfEntity(user2.state.id))

    val ev1 = persistedEvents2(0)
    val ev2 = persistedEvents2(1)
    val ev3 = persistedEvents2(2)

    ev1.event should be(eventStoreRecords2(0).event)
    ev1.entityVersion should be(1)
    ev1.shard should be(200 % 3)

    ev2.event should be(eventStoreRecords2(1).event)
    ev2.entityVersion should be(2)
    ev2.shard should be(200 % 3)

    ev3.event should be(eventStoreRecords2(2).event)
    ev3.entityVersion should be(3)
    ev3.shard should be(200 % 3)

  }

  "storeEvents" - {
    "should store events" in {

      val flow =
        valueToFunc1(create(UserId(189), "john", "john@mail.com", PendingActivation)) andThen activate andThen changeUsername(
          "johny"
        )
      val user = flow()

      val eventStoreRecords = Seq(
        NewEventStoreRecord(user.state.id, 1, user.metadata.uncommittedChanges.head),
        NewEventStoreRecord(user.state.id, 2, user.metadata.uncommittedChanges(1)),
        NewEventStoreRecord(user.state.id, 3, user.metadata.uncommittedChanges(2))
      )

      awaitResult(
        eventStore.storeEvents(
          eventStoreRecords
        )
      )

      val persistedEvents =
        awaitResult(eventStore.getEventsOfEntity(user.state.id))

      val ev1 = persistedEvents(0)
      val ev2 = persistedEvents(1)
      val ev3 = persistedEvents(2)

      ev1.event should be(eventStoreRecords(0).event)
      ev1.entityVersion should be(1)
      ev1.shard should be(189 % 3)

      ev2.event should be(eventStoreRecords(1).event)
      ev2.entityVersion should be(2)
      ev2.shard should be(189 % 3)

      ev3.event should be(eventStoreRecords(2).event)
      ev3.entityVersion should be(3)
      ev3.shard should be(189 % 3)
    }

    "trying to save event with version which already exists in the event store" - {
      "should fail with ConcurrencyException and do not store events and current state of entity whose events causes concurrency issue" in {
        val flow1 =
          valueToFunc1(create(UserId(189), "john", "john@mail.com", PendingActivation)) andThen activate andThen changeUsername(
            "jonny"
          )
        val user1 = flow1()

        val eventStoreRecords1 = Seq(
          NewEventStoreRecord(user1.state.id, 1, user1.metadata.uncommittedChanges.head),
          NewEventStoreRecord(user1.state.id, 2, user1.metadata.uncommittedChanges(1)),
          NewEventStoreRecord(user1.state.id, 3, user1.metadata.uncommittedChanges(2))
        )

        awaitResult(
          eventStore.storeEvents(
            eventStoreRecords1
          )
        )

        val flow2 = valueToFunc1(
            create(UserId(189), "jane", "jane@mail.com", PendingActivation)
          ) andThen activate

        val user2 = flow2()

        val eventStoreRecords2 = Seq(
          NewEventStoreRecord(user2.state.id, 1, user2.metadata.uncommittedChanges.head),
          NewEventStoreRecord(user2.state.id, 2, user2.metadata.uncommittedChanges(1))
        )

        Thread.sleep(2000)
        whenReady(
          eventStore
            .storeEvents(
              eventStoreRecords2
            )
            .failed
        ) { ex =>
          ex should be(a[ConcurrencyException])

          val persistedEvents =
            awaitResult(eventStore.getEventsOfEntity(user1.state.id))
          persistedEvents.size should be(3)

          val ev1 = persistedEvents(0)
          val ev2 = persistedEvents(1)
          val ev3 = persistedEvents(2)

          ev1.event should be(eventStoreRecords1(0).event)
          ev1.entityVersion should be(1)
          ev1.shard should be(189 % 3)

          ev2.event should be(eventStoreRecords1(1).event)
          ev2.entityVersion should be(2)
          ev2.shard should be(189 % 3)

          ev3.event should be(eventStoreRecords1(2).event)
          ev3.entityVersion should be(3)
          ev3.shard should be(189 % 3)
        }
      }
    }
  }
  "save events of multiple entities" in {
    val flow1 = valueToFunc1(
        create(UserId(189), "john", "john@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername(
        "jonny"
      )
    val user1 = flow1()

    val eventStoreRecords1 = Seq(
      NewEventStoreRecord(user1.state.id, 1, user1.metadata.uncommittedChanges.head),
      NewEventStoreRecord(user1.state.id, 2, user1.metadata.uncommittedChanges(1)),
      NewEventStoreRecord(user1.state.id, 3, user1.metadata.uncommittedChanges(2))
    )

    awaitResult(
      eventStore.storeEvents(
        eventStoreRecords1
      )
    )

    val flow2 = valueToFunc1(
        create(UserId(200), "bill", "bill@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername("billy")
    val user2 = flow2()

    val eventStoreRecords2 = Seq(
      NewEventStoreRecord(user2.state.id, 1, user2.metadata.uncommittedChanges.head),
      NewEventStoreRecord(user2.state.id, 2, user2.metadata.uncommittedChanges(1)),
      NewEventStoreRecord(user2.state.id, 3, user2.metadata.uncommittedChanges(2))
    )

    awaitResult(
      eventStore.storeEvents(
        eventStoreRecords2
      )
    )

    val flow3 = valueToFunc1(
        create(UserId(201), "jane", "jane@mail.com", PendingActivation)
      ) andThen activate andThen changeUsername("janny")
    val user3 = flow3()

    val eventStoreRecords3 =
      Seq(
        NewEventStoreRecord(user3.state.id, 1, user3.metadata.uncommittedChanges.head),
        NewEventStoreRecord(user3.state.id, 2, user3.metadata.uncommittedChanges(1)),
        NewEventStoreRecord(user3.state.id, 3, user3.metadata.uncommittedChanges(2))
      )

    awaitResult(
      eventStore.storeEvents(
        eventStoreRecords3
      )
    )

    val persistedEvents1 =
      awaitResult(eventStore.getEventsOfEntity(user1.state.id))

    val ev1_1 = persistedEvents1(0)
    val ev1_2 = persistedEvents1(1)
    val ev1_3 = persistedEvents1(2)

    ev1_1.event should be(persistedEvents1(0).event)
    ev1_1.entityVersion should be(1)
    ev1_1.shard should be(189 % 3)

    ev1_2.event should be(persistedEvents1(1).event)
    ev1_2.entityVersion should be(2)
    ev1_2.shard should be(189 % 3)

    ev1_3.event should be(persistedEvents1(2).event)
    ev1_3.entityVersion should be(3)
    ev1_3.shard should be(189 % 3)

    val persistedEvents2 =
      awaitResult(eventStore.getEventsOfEntity(user2.state.id))

    val ev2_1 = persistedEvents2(0)
    val ev2_2 = persistedEvents2(1)
    val ev2_3 = persistedEvents2(2)

    ev2_1.event should be(eventStoreRecords2(0).event)
    ev2_1.entityVersion should be(1)
    ev2_1.shard should be(200 % 3)

    ev2_2.event should be(eventStoreRecords2(1).event)
    ev2_2.entityVersion should be(2)
    ev2_2.shard should be(200 % 3)

    ev2_3.event should be(eventStoreRecords2(2).event)
    ev2_3.entityVersion should be(3)
    ev2_3.shard should be(200 % 3)

    val persistedEvents3 =
      awaitResult(eventStore.getEventsOfEntity(user3.state.id))

    val ev3_1 = persistedEvents3(0)
    val ev3_2 = persistedEvents3(1)
    val ev3_3 = persistedEvents3(2)

    ev3_1.event should be(eventStoreRecords3(0).event)
    ev3_1.entityVersion should be(1)
    ev3_1.shard should be(201 % 3)

    ev3_2.event should be(eventStoreRecords3(1).event)
    ev3_2.entityVersion should be(2)
    ev3_2.shard should be(201 % 3)

    ev3_3.event should be(eventStoreRecords3(2).event)
    ev3_3.entityVersion should be(3)
    ev3_3.shard should be(201 % 3)
  }

  "getEventsAfterOffset" - {
    "should return events after specific offset of given shard" in {
      val flow1 = valueToFunc1(
          create(UserId(189), "john", "john@mail.com", PendingActivation)
        ) andThen activate andThen changeUsername("jonny")
      val user1 = flow1()

      val eventStoreRecords1 = Seq(
        NewEventStoreRecord(user1.state.id, 1, user1.metadata.uncommittedChanges.head),
        NewEventStoreRecord(user1.state.id, 2, user1.metadata.uncommittedChanges(1)),
        NewEventStoreRecord(user1.state.id, 3, user1.metadata.uncommittedChanges(2))
      )

      awaitResult(
        eventStore.storeEvents(
          eventStoreRecords1
        )
      )

      val flow2 = valueToFunc1(
          create(UserId(200), "bill", "bill@mail.com", PendingActivation)
        ) andThen activate andThen changeUsername("billy")
      val user2 = flow2()

      val eventStoreRecords2 = Seq(
        NewEventStoreRecord(user2.state.id, 1, user2.metadata.uncommittedChanges.head),
        NewEventStoreRecord(user2.state.id, 2, user2.metadata.uncommittedChanges(1)),
        NewEventStoreRecord(user2.state.id, 3, user2.metadata.uncommittedChanges(2))
      )

      awaitResult(
        eventStore.storeEvents(
          eventStoreRecords2
        )
      )

      val flow3 = valueToFunc1(
          create(UserId(204), "jane", "jane@mail.com", PendingActivation)
        ) andThen activate andThen changeUsername("jany")
      val user3 = flow3()

      val eventStoreRecords3 =
        Seq(
          NewEventStoreRecord(user3.state.id, 1, user3.metadata.uncommittedChanges.head),
          NewEventStoreRecord(user3.state.id, 2, user3.metadata.uncommittedChanges(1)),
          NewEventStoreRecord(user3.state.id, 3, user3.metadata.uncommittedChanges(2))
        )

      awaitResult(
        eventStore.storeEvents(
          eventStoreRecords3
        )
      )

      val eventsAfterOffset = awaitResult(eventStore.getEventsAfterOffset(2, 0))

      // Events of user2 (with id=200) should be ignored, since their shard is 200 % 3 = 2 (and not 0)
      eventsAfterOffset.size should be(4)

      eventsAfterOffset(0).offset should be(3)
      eventsAfterOffset(0).entityId should be(
        UserId(189)
      )
      eventsAfterOffset(0).entityVersion should be(3)

      eventsAfterOffset(1).offset should be(7)
      eventsAfterOffset(1).entityId should be(
        UserId(204)
      )
      eventsAfterOffset(1).entityVersion should be(1)

      eventsAfterOffset(2).offset should be(8)
      eventsAfterOffset(2).entityId should be(
        UserId(204)
      )
      eventsAfterOffset(2).entityVersion should be(2)

      eventsAfterOffset(3).offset should be(9)
      eventsAfterOffset(3).entityId should be(
        UserId(204)
      )
      eventsAfterOffset(3).entityVersion should be(3)
    }
  }
}
