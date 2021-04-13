package domain.systemmanager

import tk.lib.core.auth.api.UserId

import java.time.Instant

case class SystemManagerState(
    id: UserId,
    roles: Seq[SystemManagerRole],
    createdAt: Instant
) extends SystemManagerBehaviour.State {
  override def applyEvent(event: SystemManagerEvent): SystemManagerState =
    event match {
      case e: SystemManagerCreated =>
        copy(
          id = e.entityId,
          roles = e.roles,
          createdAt = e.createdAt
        )
    }
}
