package domain.user

import play.api.libs.json.Format
import tk.lib.core.auth.api.UserId
import tk.lib.eventsourcing.{ EventSourcedRepository, EventSourcedRepositoryComponents }
import scala.concurrent.ExecutionContext
import jsonformat.MongoFormats._

class UserRepository(val components: EventSourcedRepositoryComponents)(
    implicit val ec: ExecutionContext
) extends EventSourcedRepository {
  override val collectionName: String                = "userEvents"
  override val numShards: Int                        = 3
  override val entityBehaviour: UserBehaviour.type   = UserBehaviour
  override val idMongoDbFormat: Format[UserId]       = userIdFormat
  override val eventMongoDbFormat: Format[UserEvent] = userEventFormat
}
