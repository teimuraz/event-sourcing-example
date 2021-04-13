package domain.systemmanager

import play.api.libs.json.Format
import tk.lib.eventsourcing.{ EventSourcedRepository, EventSourcedRepositoryComponents }

import scala.concurrent.ExecutionContext
import jsonformat.MongoFormats._
import tk.lib.core.auth.api.UserId

class SystemManagerRepository(val components: EventSourcedRepositoryComponents)(
    implicit val ec: ExecutionContext
) extends EventSourcedRepository {
  override val collectionName: String                         = "systemManagerEvents"
  override val numShards: Int                                 = 3
  override val entityBehaviour: SystemManagerBehaviour.type   = SystemManagerBehaviour
  override val idMongoDbFormat: Format[UserId]                = userIdFormat
  override val eventMongoDbFormat: Format[SystemManagerEvent] = systemManagerEventFormat
}
