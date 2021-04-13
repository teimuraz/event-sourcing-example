package domain.organization

import play.api.libs.json.Format
import tk.lib.eventsourcing.{ EventSourcedRepository, EventSourcedRepositoryComponents }
import scala.concurrent.ExecutionContext
import jsonformat.MongoFormats._

class OrganizationRepository(val components: EventSourcedRepositoryComponents)(
    implicit val ec: ExecutionContext
) extends EventSourcedRepository {
  override val collectionName: String                        = "organizationEvents"
  override val numShards: Int                                = 3
  override val entityBehaviour: OrganizationBehaviour.type   = OrganizationBehaviour
  override val idMongoDbFormat: Format[OrganizationId]       = organizationIdFormat
  override val eventMongoDbFormat: Format[OrganizationEvent] = organizationEventFormat
}
