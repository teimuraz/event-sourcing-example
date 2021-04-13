package module

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.{ FirebaseApp, FirebaseOptions }
import com.google.firebase.auth.FirebaseAuth
import com.softwaremill.macwire.wire
import tk.lib.core.CoreComponents
import tk.lib.core.auth.api.AuthService
import tk.lib.core.auth.impl.AuthServiceImpl
import tk.lib.core.concurrent.BlockingIoExecutionContext
import tk.lib.core.config.ConfigBuilder
import tk.lib.core.firebase.FirebaseAuthFacade
import tk.lib.core.google.GoogleGeneralConfig
import tk.lib.core.model.MongoDbRepositoryComponents
import tk.lib.eventsourcing.idgenerator.NumericIdGenerator
import tk.lib.eventsourcing.{
  EventSourcedRepositoryComponents,
  EventSourcingComponents,
  EventStorePubsubConnector,
  OffsetGenerator,
  PubsubConnectorOffsetManger
}
import config.ApplicationConfig
import config.GoogleConfig.PubsubConfig
import play.api.{ NoHttpFiltersComponents, OptionalSourceMapper }
import play.api.libs.concurrent.AkkaComponents
import play.modules.reactivemongo.ReactiveMongoApiComponents
import reactivemongo.api.DB
import router.RouterComponents
import tk.lib.pubsub.PubsubAdmin

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait EssentialComponents
    extends AkkaComponents
    with ReactiveMongoApiComponents
    with NoHttpFiltersComponents
    with CoreComponents
    with EventSourcingComponents {

  implicit lazy val system: ActorSystem = actorSystem
  implicit lazy val mat: Materializer   = materializer

  // Router
  val optionalSourceMapper: OptionalSourceMapper

  val routeComponents: RouterComponents

  /// Wrap execution context used by blocking io to avoid accident passing of main (non-blocking) execution context
  /// into blocking operations.

  implicit lazy val blockingExecutionContext: BlockingIoExecutionContext =
    BlockingIoExecutionContext(
      actorSystem.dispatchers.lookup("blocking-io-context")
    )

  /// Setup firebase

  lazy val firebaseAuth: FirebaseAuth = {
    val options: FirebaseOptions = FirebaseOptions
      .builder()
      .setCredentials(GoogleCredentials.getApplicationDefault)
      .build

    if (FirebaseApp.getApps.size() == 0) {
      FirebaseApp.initializeApp(options)
    }
    FirebaseAuth.getInstance()
  }

  lazy val firebaseAuthFacade: FirebaseAuthFacade = wire[FirebaseAuthFacade]

  /// Auth

  lazy val authService: AuthService = wire[AuthServiceImpl]

  /// Config

  lazy val config: ApplicationConfig =
    ConfigBuilder.build[ApplicationConfig]("application").get

  lazy val googleGeneralConfig: GoogleGeneralConfig = config.google.general

  lazy val pubsubConfig: PubsubConfig = config.google.pubsub

  /// Mongodb

  // We can block on startup
  lazy val database: DB = Await.result(reactiveMongoApi.database, 3.minutes)

  lazy val mongoDbRepositoryComponents: MongoDbRepositoryComponents =
    wire[MongoDbRepositoryComponents]

  /// Event sourcing components

  lazy val offsetGenerator: OffsetGenerator = wire[OffsetGenerator]

  lazy val idGenerator: NumericIdGenerator = wire[NumericIdGenerator]

  lazy val eventSourcedRepositoryComponents: EventSourcedRepositoryComponents =
    wire[EventSourcedRepositoryComponents]

  override lazy val pubsubConnectorOffsetManger: PubsubConnectorOffsetManger =
    wire[PubsubConnectorOffsetManger]

  lazy val eventStorePubsubConnectorConfig: EventStorePubsubConnector.Config =
    config.eventStorePubsubConnector

  lazy val pubsubAdmin = wire[PubsubAdmin]
}
