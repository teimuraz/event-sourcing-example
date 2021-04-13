package router

import akka.stream.Materializer
import command.SignUpCommand.{ SignUpRequest, SignUpResult }
import play.api.libs.json.Reads
import play.api.routing.Router
import sttp.tapir.endpoint
import sttp.tapir.json.play.jsonBody

import scala.concurrent.ExecutionContext
import sttp.tapir._
import sttp.tapir.server.play._
import play.api.routing.Router.Routes
import codec.Codecs._
import command.SignUpCommand
import jsonformat.DefaultFormats._

class UserRouter(signUpCommand: SignUpCommand, rc: RouterComponents)(
    implicit override val materializer: Materializer,
    ec: ExecutionContext
) extends BaseRouter(rc) {

  implicit val trimStringReads: Reads[String] = Reads.StringReads.map(_.trim)

  private val signUpUserEndpoint =
    endpoint.post
      .in("v1" / "users" / "signup")
      .in(jsonBody[SignUpRequest])
      .out(jsonBody[SignUpResult])

  private val signUpUserRoute: Routes =
    PlayServerInterpreter.toRoute(signUpUserEndpoint) { req =>
      unAuthenticated {
        signUpCommand.execute(req)
      }
    }

  override def endpoints: Seq[Endpoint[_, _, _, _]] = Seq(signUpUserEndpoint)

  override def routes: Router.Routes =
    signUpUserRoute
}
