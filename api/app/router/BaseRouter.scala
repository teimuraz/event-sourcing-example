package router

import akka.stream.Materializer
import tk.lib.core.auth.api.AuthenticatedContext
import play.api.libs.json.{ Json, OFormat }
import play.api.routing.SimpleRouter
import sttp.tapir.Endpoint
import sttp.tapir.json.play.jsonBody
import sttp.tapir.server.{
  DecodeFailureHandling,
  DefaultDecodeFailureResponse,
  ServerDefaults
}
import sttp.tapir.server.play.PlayServerOptions
import scala.concurrent.{ ExecutionContext, Future }
import sttp.tapir.generic.auto._
import sttp.tapir.{ endpoint, _ }

abstract class BaseRouter(rc: RouterComponents)(
    implicit val materializer: Materializer,
    ec: ExecutionContext
) extends SimpleRouter {

  def endpoints: Seq[Endpoint[_, _, _, _]]

  def failureResponse(
      response: DefaultDecodeFailureResponse,
      message: String
  ): DecodeFailureHandling =
    DecodeFailureHandling.response(
      ServerDefaults.failureOutput(jsonBody[BaseRouter.Failure])
    )(
      (response, BaseRouter.Failure(message))
    )

  val decodeFailureHandler = ServerDefaults.decodeFailureHandler.copy(
    response = failureResponse
  )

  implicit val serverOptions =
    PlayServerOptions.default.copy(decodeFailureHandler = decodeFailureHandler)

  val authEndpoint = endpoint
    .in(auth.bearer[String]())

  val unAuthEndpoint = endpoint

  def authenticated[T](
      token: String
  )(block: AuthenticatedContext => Future[T]): Future[Either[Unit, T]] =
    for {
      identity <- rc.authService.identify(token)
      context = AuthenticatedContext(identity = identity, ip = None)
      result <- block(context)
    } yield {
      Right(result)
    }

  def unAuthenticated[T](block: => Future[T]): Future[Either[Unit, T]] =
    block.map(result => Right(result))

}

object BaseRouter {
  case class Failure(message: String)

  object Failure {
    implicit val format: OFormat[Failure] = Json.format[Failure]
  }
}
