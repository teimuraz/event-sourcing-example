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

package tk.lib.core.validation

import tk.lib.core.error.{ InternalErrorException, ValidationException }
import tk.lib.core.validation.Validation.ValidationResult
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

package object syntax {
  implicit class ValidationResultSyntax[R](result: ValidationResult[R]) {
    def toTryWithInternalErrorException(message: String): Try[R] = result match {
      case Left(error) =>
        Failure(new InternalErrorException(s"$message | ${error.message}"))
      case Right(v) => Success(v)
    }

    def toTryValidationException(message: String): Try[R] = result match {
      case Left(error) =>
        Failure(new InternalErrorException(s"$message | ${error.message}"))
      case Right(v) => Success(v)
    }

    def toFutureWithValidationException: Future[R] = result match {
      case Left(error) =>
        Future.failed(new ValidationException(error.message))
      case Right(v) => Future.successful(v)
    }
  }
}
