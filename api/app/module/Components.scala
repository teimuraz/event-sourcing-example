package module

import akka.Done
import akka.actor.CoordinatedShutdown
import tk.lib.eventsourcing.EventStorePubsubConnectorsRunner
import com.softwaremill.macwire.wire
import play.api.ApplicationLoader.Context
import play.api.{ Logging, OptionalSourceMapper }
import play.api.http.HttpErrorHandler
import play.api.routing.Router
import play.modules.reactivemongo.ReactiveMongoApiFromContext
import router.{ ApiRouter, RouterComponents }

import scala.concurrent.Future

class Components(context: Context)
    extends ReactiveMongoApiFromContext(context)
    with EssentialComponents
    with UserComponents
    with OrganizationComponents
    with Logging {

  override lazy val optionalSourceMapper: OptionalSourceMapper =
    wire[OptionalSourceMapper]

  override lazy val routeComponents: RouterComponents = wire[RouterComponents]

  override lazy val httpErrorHandler: HttpErrorHandler =
    wire[ErrorHandler]

  lazy val apiRouter: ApiRouter = wire[ApiRouter]

  override def router: Router = apiRouter

  /// Setup event store pubsub connectors

  lazy val eventStorePubsubConnectorsRunner: EventStorePubsubConnectorsRunner =
    EventStorePubsubConnectorsRunner(
      actorSystem,
      eventStorePubsubConnectorConfig,
      Seq(
        userPubsubConnector,
        systemManagerPubsubConnector,
        organizationPubsubConnector
      )
    )

  lazy val googleResourcesCreator: GoogleResourcesCreator = wire[GoogleResourcesCreator]

  /// Bootstrap

  def onStart() =
    googleResourcesCreator.create().map { _ =>
      userEventConsumer.run()
      systemManagerEventConsumer.run()
      organizationEventConsumer.run()
      eventStorePubsubConnectorsRunner.run()
    }

  /// Cleanup

  def onShutdown(): Future[Boolean] =
    for {
      _ <- producer.cleanup()
      _ <- organizationEventConsumer.stop()
      _ <- userEventConsumer.stop()
      _ <- systemManagerEventConsumer.stop()
    } yield {
      true
    }

  onStart().recoverWith {
    case e =>
      logger.error(s"Error running startup scripts, error: ${e.getMessage}", e)
      throw e
  }

  coordinatedShutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "free") { () =>
    onShutdown().map(_ => Done)
  }
}
