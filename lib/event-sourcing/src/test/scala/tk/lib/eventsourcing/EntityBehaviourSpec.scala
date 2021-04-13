/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing

import tk.lib.core.testutil.BaseSpec
import tk.lib.eventsourcing.UserEvent.{ UserActivated, UserCreated, UsernameChanged }
import tk.lib.eventsourcing.UserStatus.PendingActivation

class EntityBehaviourSpec extends BaseSpec {

  "test events flow" in {
    val user = UserBehaviour.create(
      UserId(999),
      "john",
      "john@mail.com",
      PendingActivation
    )

    user.state should be(
      UserState(
        UserId(999),
        "john",
        "john@mail.com",
        UserStatus.PendingActivation
      )
    )
    user.metadata.currentVersion should be(1)
    user.metadata.persistedVersion should be(0)
    user.metadata.uncommittedChanges.size should be(1)
    user.metadata.uncommittedChanges.last should be(
      UserCreated(
        UserId(999),
        "john",
        "john@mail.com",
        UserStatus.PendingActivation
      )
    )

    val activatedUser = UserBehaviour.activate(user)

    activatedUser.state should be(
      UserState(
        UserId(999),
        "john",
        "john@mail.com",
        UserStatus.Active
      )
    )
    activatedUser.metadata.currentVersion should be(2)
    activatedUser.metadata.persistedVersion should be(0)
    activatedUser.metadata.uncommittedChanges.size should be(2)
    activatedUser.metadata.uncommittedChanges.last should be(
      UserActivated(UserId(999), UserStatus.Active)
    )

    val userWithChangedUsername = UserBehaviour.changeUsername("jonny")(activatedUser)

    userWithChangedUsername.state should be(
      UserState(
        UserId(999),
        "jonny",
        "john@mail.com",
        UserStatus.Active
      )
    )
    userWithChangedUsername.metadata.currentVersion should be(3)
    userWithChangedUsername.metadata.persistedVersion should be(0)
    userWithChangedUsername.metadata.uncommittedChanges.size should be(3)
    userWithChangedUsername.metadata.uncommittedChanges.last should be(
      UsernameChanged(UserId(999), "jonny")
    )
  }
}
