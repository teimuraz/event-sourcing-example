/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing

import tk.lib.core.model.{ EntityLike, MongoDbRepository, MongoDbRepositoryComponents }
import tk.lib.eventsourcing.UserProjectionRepository.UserProjection
import play.api.libs.json.{ Format, Json, OFormat }
import scala.concurrent.{ ExecutionContext, Future }

class UserProjectionRepository(val components: MongoDbRepositoryComponents)(
    implicit val ec: ExecutionContext
) extends tk.lib.core.model.Repository[UserProjection, UserId]
    with MongoDbRepository[UserProjection, UserId] {

  override def collectionName: String = "userProjections"

  override implicit val idFormat: Format[UserId]                       = UserId.format
  override implicit val entityFormat: Format[UserProjection]           = UserProjection.format
  override implicit val entityLike: EntityLike[UserProjection, UserId] = EntityLike.auto
}

object UserProjectionRepository {
  case class UserProjection(
      id: UserId,
      username: String,
      email: String,
      status: UserStatus,
      metadata: tk.lib.core.model.Metadata
  )

  object UserProjection {
    implicit lazy val format: OFormat[UserProjection] = Json.format[UserProjection]
  }
}
