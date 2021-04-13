package command

import tk.lib.core.auth.api.UserId
import domain.systemmanager.{
  SystemManagerBehaviour,
  SystemManagerRepository,
  SystemManagerRole
}

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

class CreateSystemManagerCommand(systemManagerRepository: SystemManagerRepository)(
    implicit ec: ExecutionContext
) {

  def execute(): Future[SystemManagerBehaviour.Entity] = {
    val systemManager = SystemManagerBehaviour.create(
      UserId(1617712928569L),
      Seq(SystemManagerRole.Admin),
      Instant.now
    )

    systemManagerRepository.store(systemManager)

  }
}
