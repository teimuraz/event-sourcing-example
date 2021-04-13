package query.organization

import tk.lib.core.model.{ EntityLike, MongoDbRepository, MongoDbRepositoryComponents }
import domain.organization.OrganizationId
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }
import jsonformat.MongoFormats._

class OrganizationProjectionRepository(val components: MongoDbRepositoryComponents)(
    implicit val ec: ExecutionContext
) extends MongoDbRepository[OrganizationProjection, OrganizationId] {

  override def collectionName: String = "organizationProjections"

  override implicit val idFormat: Format[OrganizationId] = organizationIdFormat
  override implicit val entityFormat: Format[OrganizationProjection] =
    organizationProjectionFormat
  override implicit val entityLike: EntityLike[OrganizationProjection, OrganizationId] =
    OrganizationProjection.entityLike

  def findByName(name: String): Future[Option[OrganizationProjection]] = {
    val selector = Json.obj("name" -> name)
    findBySelector(selector)
  }
}
