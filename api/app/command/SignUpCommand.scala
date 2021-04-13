package command

import com.google.firebase.auth.{ FirebaseAuth, FirebaseAuthException }
import tk.lib.core.datetime.DateTimeUtils
import tk.lib.core.error.ValidationException
import tk.lib.core.firebase.FirebaseAuthFacade
import SignUpCommand.{ SignUpRequest, SignUpResult }
import domain.user.{ UserBehaviour, UserRepository, UserState }
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }
import jsonformat.DefaultFormats._

/**
  * Create user both locally and in firebase.
  * Add local user id to custom claims in firebase so that local user id will always be embedded to jwt token.
  * Returns created user and firebase custom token so it can be used to sign in user immediately after sign up.
  */
class SignUpCommand(
    userRepository: UserRepository,
    firebaseAuthFacade: FirebaseAuthFacade,
    firebaseAuth: FirebaseAuth,
    dateTimeUtils: DateTimeUtils
)(implicit ec: ExecutionContext) {

  def execute(req: SignUpRequest): Future[SignUpResult] =
    for {
      firebaseUser <- firebaseAuthFacade
        .createUser(req.email, req.password)
        .recoverWith {
          case e @ (_: FirebaseAuthException | _: IllegalArgumentException) =>
            Future.failed(new ValidationException(e.getMessage, Some(e)))
        }
      userId <- userRepository.nextId()
      user = UserBehaviour.create(
        id = userId,
        email = req.email,
        firstName = None,
        lastName = None,
        firebaseUser.getUid,
        dateTimeUtils.now
      )
      _ <- userRepository.store(user)
      _ <- firebaseAuthFacade.setCustomUserClaims(
        firebaseUser.getUid,
        java.util.Map.of("appUserId", user.state.id.value.toString)
      )
      firebaseSignInToken = generateFirebaseCustomToken(firebaseUser.getUid)
    } yield {
      SignUpResult(user.state, firebaseSignInToken)
    }

  private[command] def generateFirebaseCustomToken(firebaseUserId: String): String =
    firebaseAuth.createCustomToken(firebaseUserId)
}

object SignUpCommand {

  case class SignUpRequest(email: String, password: String)

  object SignUpRequest {
    implicit lazy val format: Format[SignUpRequest] = Json.format[SignUpRequest]
  }

  case class SignUpResult(user: UserState, firebaseCustomToken: String)

  object SignUpResult {
    implicit lazy val format: Format[SignUpResult] = Json.format[SignUpResult]
  }
}
