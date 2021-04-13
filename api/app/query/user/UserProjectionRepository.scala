package query.user

import tk.lib.core.auth.api.UserId
import tk.lib.core.model.{ EntityLike, MongoDbRepository, MongoDbRepositoryComponents }
import jsonformat.MongoFormats
import play.api.libs.json.Format

import scala.concurrent.ExecutionContext

class UserProjectionRepository(val components: MongoDbRepositoryComponents)(
    implicit val ec: ExecutionContext
) extends MongoDbRepository[UserProjection, UserId] {
  override def collectionName: String = "userProjections"

  override implicit val idFormat: Format[UserId] = UserId.format
  override implicit val entityFormat: Format[UserProjection] =
    MongoFormats.userProjectionFormat
  override implicit val entityLike: EntityLike[UserProjection, UserId] =
    UserProjection.entityLike
}
