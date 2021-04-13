package domain.systemmanager

import tk.lib.core.auth.api.UserId
import tk.lib.eventsourcing.Event

import java.time.Instant

sealed trait SystemManagerEvent extends Event[UserId]

case class SystemManagerCreated(
    entityId: UserId,
    roles: Seq[SystemManagerRole],
    createdAt: Instant
) extends SystemManagerEvent

case class SystemManagerRoleAssigned(id: UserId, role: SystemManagerRole)
