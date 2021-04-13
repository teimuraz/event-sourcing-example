package module

import tk.lib.core.error.{
  ForbiddenException,
  NotFoundException,
  UnauthorizedException,
  ValidationException
}
import play.api.http.JsonHttpErrorHandler
import play.api.mvc.{ RequestHeader, Result }
import play.api.{ Environment, Logging, OptionalSourceMapper }

import scala.concurrent.Future

class ErrorHandler(
    env: Environment,
    sourceMapper: OptionalSourceMapper
) extends JsonHttpErrorHandler(env, sourceMapper)
    with Logging {

  override def onServerError(
      request: RequestHeader,
      exception: Throwable
  ): Future[Result] = {
    val status = exception match {
      case _: ValidationException   => 400
      case _: UnauthorizedException => 401
      case _: ForbiddenException    => 403
      case _: NotFoundException     => 404
      case _: Throwable             => 500
    }

    if (status >= 400 && status < 500) {
      onClientError(request, status, exception.getMessage)
    } else {
      logger.error(s"Error: ${exception.getMessage} | Request: $request", exception)
      super.onServerError(request, exception)
    }
  }
}
