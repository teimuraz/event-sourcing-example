/*
 *
 * Copyright 2021 TK
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package tk.lib.core.auth.impl

import tk.lib.core.auth.api.{ AuthService, AuthenticatedIdentity, UserId }
import tk.lib.core.error.{ UnauthorizedException, ValidationException }

import scala.jdk.CollectionConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Try }
import com.google.firebase.auth.{ FirebaseAuth, FirebaseAuthException }
import tk.lib.core.validation.syntax._
import tk.lib.core.auth.api.{ AuthService, AuthenticatedIdentity, UserId }
import play.api.Logging
import play.api.libs.json.Json

class AuthServiceImpl(firebaseAuth: FirebaseAuth)(
    implicit ec: ExecutionContext
) extends AuthService
    with Logging {

  override def identify(
      token: String
  ): Future[AuthenticatedIdentity] =
    Future.fromTry(decodeToken(token))

  private[impl] def decodeToken(
      token: String
  ): Try[AuthenticatedIdentity] =
    Try {
      // TODO:: Make sure this doesn't perform remote call, otherwise we need to execute in dedicated, blocking
      // execution context
      val decodedToken = firebaseAuth.verifyIdToken(token)
      val claims       = decodedToken.getClaims.asScala.toMap
      val rawUserId = claims
        .getOrElse(
          "appUserId",
          throw new ValidationException(
            s"appUserId must be provided in token's custom claims"
          )
        )
        .toString
        .toLong

      val userId = UserId(rawUserId)

      AuthenticatedIdentity(userId, decodedToken.getUid, Json.obj())
    }.recoverWith {
      case e @ (_: FirebaseAuthException | _: IllegalArgumentException) =>
        logger.warn(e.getMessage, e)
        Failure(
          new UnauthorizedException(
            s"Invalid access token or access token has been expired"
          )
        )
    }
}
