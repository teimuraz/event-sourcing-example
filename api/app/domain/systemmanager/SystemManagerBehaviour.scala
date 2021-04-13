package domain.systemmanager

import java.time.Instant
import tk.lib.core.auth.api.UserId
import tk.lib.eventsourcing.EntityBehaviour

object SystemManagerBehaviour extends EntityBehaviour {

  def create(
      id: UserId,
      roles: Seq[SystemManagerRole],
      createdAt: Instant
  ): SystemManagerBehaviour.Entity =
    emptyEntity.applyChange(SystemManagerCreated(id, roles, createdAt))

  override type Id  = UserId
  override type Evt = SystemManagerEvent
  override type S   = SystemManagerState

  override val emptyState: SystemManagerState =
    SystemManagerState(UserId(0), Nil, Instant.now())
}
