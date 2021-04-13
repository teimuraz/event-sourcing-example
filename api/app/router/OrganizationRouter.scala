package router

import akka.stream.Materializer
import play.api.routing.Router
import play.api.routing.Router.Routes
import sttp.tapir.{ endpoint, _ }
import sttp.tapir.json.play.jsonBody
import sttp.tapir.server.play._
import domain.organization.OrganizationState
import codec.Codecs._
import command.AddMemberToOrganizationCommand.AddMemberToOrganizationRequest
import command.{ AddMemberToOrganizationCommand, CreateOrganizationCommand }
import command.CreateOrganizationCommand.CreateOrganizationRequest

import scala.concurrent.ExecutionContext
import jsonformat.DefaultFormats._

class OrganizationRouter(
    createOrganizationCommand: CreateOrganizationCommand,
    addMemberToOrganizationCommand: AddMemberToOrganizationCommand,
    rc: RouterComponents
)(
    implicit override val materializer: Materializer,
    ec: ExecutionContext
) extends BaseRouter(rc) {

  private val createOrganizationEndpoint =
    authEndpoint.post
      .in("v1" / "organizations" / "create")
      .in(jsonBody[CreateOrganizationRequest])
      .out(jsonBody[OrganizationState])

  private val createOrganizationRoute: Routes =
    PlayServerInterpreter.toRoute(createOrganizationEndpoint) {
      case (token, req) =>
        authenticated(token) { implicit context =>
          createOrganizationCommand.execute(req)
        }
    }

  private val addMemberToOrganizationEndpoint =
    authEndpoint.post
      .in("v1" / "organizations" / "add-member")
      .in(jsonBody[AddMemberToOrganizationRequest])
      .out(jsonBody[OrganizationState])

  private val addMemberToOrganizationRoute: Routes =
    PlayServerInterpreter.toRoute(addMemberToOrganizationEndpoint) {
      case (token, req) =>
        authenticated(token) { implicit context =>
          addMemberToOrganizationCommand.execute(req)
        }
    }

  override def endpoints: Seq[Endpoint[_, _, _, _]] =
    Seq(createOrganizationEndpoint, addMemberToOrganizationEndpoint)

  override def routes: Router.Routes =
    createOrganizationRoute.orElse(addMemberToOrganizationRoute)
}
