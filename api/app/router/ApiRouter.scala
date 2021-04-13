package router

import akka.stream.Materializer
import play.api.routing.{ Router, SimpleRouter }
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import scala.concurrent.ExecutionContext
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.play.SwaggerPlay
import sttp.tapir.server.play._

class ApiRouter(
    userRouter: UserRouter,
    organizationRouter: OrganizationRouter
)(
    implicit val materializer: Materializer,
    ec: ExecutionContext
) extends SimpleRouter {

  val docs: String =
    OpenAPIDocsInterpreter
      .toOpenAPI(
        List(userRouter.endpoints, organizationRouter.endpoints).flatten,
        "TK",
        "1.0"
      )
      .toYaml

  val docsRoute: Router.Routes = new SwaggerPlay(docs).routes

  override def routes: Router.Routes =
    userRouter.routes.orElse(organizationRouter.routes).orElse(docsRoute)
}
