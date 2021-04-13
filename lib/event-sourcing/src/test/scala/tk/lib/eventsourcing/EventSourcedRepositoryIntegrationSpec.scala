/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing
import tk.lib.core.functionutils.valueToFunc1
import tk.lib.eventsourcing.UserStatus.PendingActivation
import UserBehaviour._

class EventSourcedRepositoryIntegrationSpec extends IntegrationSpecLike {

  "find" - {
    "when entity  does not exists" - {
      "should return None" in {
        val userOpt =
          awaitResult(userRepository.find(UserId(1828389)))
        userOpt should be(None)
      }
    }
    "entity exists" - {
      "should return it" in {

        val flow = valueToFunc1(
            create(UserId(999), "john", "john@mail.com", PendingActivation)
          ) andThen activate andThen changeUsername("jonny")

        val user = flow()

        awaitResult(userRepository.store(user))

        val userFromRepo =
          awaitResult(userRepository.find(UserId(999))).get

        userFromRepo.state.id should be(UserId(999))
        userFromRepo.state.username should be("jonny")
        userFromRepo.state.email should be("john@mail.com")
        userFromRepo.metadata.currentVersion should be(3)
        userFromRepo.metadata.persistedVersion should be(3)
        userFromRepo.metadata.uncommittedChanges should be(Nil)
      }
    }
  }

  "store" - {
    "should store entity to the repository and update metadata" in {
      val flow = valueToFunc1(
          create(UserId(999), "john", "john@mail.com", PendingActivation)
        ) andThen activate andThen changeUsername("jonny")

      val user = flow()

      val storedUser =
        awaitResult(userRepository.store(user))

      storedUser.metadata.currentVersion should be(3)
      storedUser.metadata.persistedVersion should be(3)
      storedUser.metadata.uncommittedChanges should be(Nil)

      val userFromRepo =
        awaitResult(userRepository.find(UserId(999))).get

      userFromRepo.state.id should be(UserId(999))
      userFromRepo.state.username should be("jonny")
      userFromRepo.state.email should be("john@mail.com")
      userFromRepo.metadata.currentVersion should be(3)
      userFromRepo.metadata.persistedVersion should be(3)
      userFromRepo.metadata.uncommittedChanges should be(Nil)

      val persistedEvents =
        awaitResult(eventStore.getEventsOfEntity(user.state.id))

      persistedEvents.size should be(3)

      val ev1 = persistedEvents(0)
      val ev2 = persistedEvents(1)
      val ev3 = persistedEvents(2)

      ev1.event should be(user.metadata.uncommittedChanges(0))
      ev1.entityVersion should be(1)
      ev1.shard should be(999 % 3)

      ev2.event should be(user.metadata.uncommittedChanges(1))
      ev2.entityVersion should be(2)
      ev2.shard should be(999 % 3)

      ev3.event should be(user.metadata.uncommittedChanges(2))
      ev3.entityVersion should be(3)
      ev3.shard should be(999 % 3)
    }
    "store entity correctly multiple times" in {
      val flow = valueToFunc1(
          create(UserId(999), "john", "john@mail.com", PendingActivation)
        ) andThen activate andThen changeUsername("jonny")
      val user       = flow()
      val storedUser = awaitResult(userRepository.store(user))

      val userModified = changeUsername("jameson")(storedUser)
      userModified.metadata.persistedVersion should be(3)
      userModified.metadata.currentVersion should be(4)
      userModified.metadata.uncommittedChanges.size should be(1)

      val storedAgain = awaitResult(userRepository.store(userModified))
      storedAgain.metadata.persistedVersion should be(4)
      storedAgain.metadata.currentVersion should be(4)
      storedAgain.metadata.uncommittedChanges should be(Nil)

      val foundUser =
        awaitResult(userRepository.find(UserId(999))).get
      foundUser.metadata.persistedVersion should be(4)
      foundUser.metadata.currentVersion should be(4)
      foundUser.metadata.uncommittedChanges should be(Nil)
    }
    "raise concurrency exception if entity is modified by multiple processes simultaneously" in {
      val userId = UserId(999)
      val flow = valueToFunc1(create(userId, "john", "john@mail.com", PendingActivation)) andThen activate andThen changeUsername(
          "jonny"
        )
      val user = flow()

      awaitResult(userRepository.store(user))

      val userOfProcess1 = awaitResult(userRepository.find(userId)).get

      val userOfProcess2 = awaitResult(userRepository.find(userId)).get

      val modifiedUserOfProcess1 = changeUsername("username2")(userOfProcess1)

      val modifiedUserOrProcess2 = changeUsername("username3")(userOfProcess2)

      awaitResult(userRepository.store(modifiedUserOfProcess1))

      whenReady(userRepository.store(modifiedUserOrProcess2).failed) { ex =>
        ex should be(a[ConcurrencyException])
      }
    }
  }
}
