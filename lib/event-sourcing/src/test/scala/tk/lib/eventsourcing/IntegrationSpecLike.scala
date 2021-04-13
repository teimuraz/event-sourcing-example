/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.eventsourcing

import akka.actor.ActorSystem
import tk.lib.core.datetime.DateTimeUtils
import com.typesafe.config.{ Config, ConfigFactory }
import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.core.model.MongoDbRepositoryComponents

import scala.concurrent.ExecutionContext.Implicits.global
import tk.lib.core.testutil.SpecLike
import tk.lib.eventsourcing.idgenerator.NumericIdGenerator
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import play.api.Logging
import play.api.libs.json.Format
import reactivemongo.api.{ DB, MongoConnection }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.reflect.runtime.universe

trait IntegrationSpecLike
    extends SpecLike
    with AnyFreeSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Logging {

  val database = awaitResult(obtainDb)

  private val configFile = "application.test.conf"

  protected lazy val config: Config =
    ConfigFactory.parseResources(configFile)

  implicit lazy val actorSystem: ActorSystem = ActorSystem()
  lazy val googleConfig: GoogleGeneralConfig = GoogleGeneralConfig("test-project")
  lazy val eventShardComputer                = new EventShardComputer

  lazy val offsetGenerator: OffsetGenerator = new OffsetGenerator(database)

  lazy val idGenerator: NumericIdGenerator = new NumericIdGenerator

  lazy val eventSourcedRepositoryComponents: EventSourcedRepositoryComponents =
    EventSourcedRepositoryComponents(
      database,
      eventShardComputer,
      dateTimeUtils,
      offsetGenerator,
      idGenerator
    )

  lazy val userRepository = new EventSourcedRepository {
    override val components: EventSourcedRepositoryComponents =
      eventSourcedRepositoryComponents
    override val collectionName: String              = "userEvents"
    override val numShards: Int                      = 3
    override val entityBehaviour: UserBehaviour.type = UserBehaviour
    override implicit val ec: ExecutionContext =
      scala.concurrent.ExecutionContext.Implicits.global
    override val idMongoDbFormat: Format[UserId]       = UserId.format
    override val eventMongoDbFormat: Format[UserEvent] = UserEvent.format
//    override implicit val idTypeTag: universe.TypeTag[UserId] =
//      implicitly[universe.TypeTag[UserId]]
  }

  lazy val eventStore = userRepository.eventStore

  lazy val mongoDbRepositoryComponents =
    MongoDbRepositoryComponents(database, dateTimeUtils)

  lazy val userProjectionRepository = new UserProjectionRepository(
    mongoDbRepositoryComponents
  )

  lazy val dateTimeUtils: DateTimeUtils = DateTimeUtils

  lazy val eventStorePubsubConnectorConfig: EventStorePubsubConnector.Config =
    EventStorePubsubConnector.Config(300.milliseconds, false)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    logger.info("Applying database migrations for tests")
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    Seq("testUsers", "userEvents", "offsets", "pubsubConnectors", "userProjections").map(
      collectionName => awaitResult(database.collection(collectionName).drop())
    )
    eventStore.markAsUnprepared()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
    database.connection.close()(1.minute)
  }

  def obtainDb: Future[DB] = {
    val driver   = new reactivemongo.api.AsyncDriver
    val mongoUri = "mongodb://localhost:27018/tk-test"
    for {
      uri        <- MongoConnection.fromString(mongoUri)
      connection <- driver.connect(uri)
      databaseName = uri.db.get
      database <- connection.database(databaseName)
    } yield {
      database
    }
  }
}
