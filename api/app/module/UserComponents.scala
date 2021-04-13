package module

import com.softwaremill.macwire.wire
import command.{ CreateSystemManagerCommand, SignUpCommand }
import command.support.{ PermissionUtils, UserUtils }
import domain.systemmanager.{ SystemManagerBehaviour, SystemManagerRepository }
import domain.user.{ UserBehaviour, UserRepository }
import query.user.{
  SystemManagerEventConsumer,
  UserEventConsumer,
  UserProjectionRepository
}
import jsonformat.DefaultFormats._
import tk.lib.eventsourcing.EventStore.EventStoreRecordDefaultFormat._
import router.UserRouter
import tk.lib.eventsourcing.EventStorePubsubConnector

trait UserComponents extends EssentialComponents {

  /// Permission utils

  lazy val permissionUtils: PermissionUtils = wire[PermissionUtils]

  /// User

  lazy val userUtils: UserUtils = wire[UserUtils]

  lazy val userRepository: UserRepository = wire[UserRepository]

  lazy val userPubsubConnector =
    new EventStorePubsubConnector[UserBehaviour.Id, UserBehaviour.Evt](
      "userEvents",
      userRepository.eventStore,
      eventStorePubsubConnectorComponents
    )

  lazy val userEventConsumer: UserEventConsumer =
    wire[UserEventConsumer]

  lazy val userProjectionRepository: UserProjectionRepository =
    wire[UserProjectionRepository]

  /// SystemManager

  lazy val systemManagerRepository: SystemManagerRepository =
    wire[SystemManagerRepository]

  lazy val systemManagerPubsubConnector =
    new EventStorePubsubConnector[SystemManagerBehaviour.Id, SystemManagerBehaviour.Evt](
      "systemManagerEvents",
      systemManagerRepository.eventStore,
      eventStorePubsubConnectorComponents
    )

  lazy val systemManagerEventConsumer: SystemManagerEventConsumer =
    wire[SystemManagerEventConsumer]

  lazy val createSystemManagerCommand: CreateSystemManagerCommand =
    wire[CreateSystemManagerCommand]

  /// Commands

  lazy val signUpCommand: SignUpCommand = wire[SignUpCommand]

  /// Router

  lazy val userRouter: UserRouter = wire[UserRouter]
}
