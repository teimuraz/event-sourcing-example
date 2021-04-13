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

package tk.lib.core.eitherutils

import tk.lib.core.error.InternalErrorException
import scala.util.{ Failure, Success, Try }

package object syntax {

  implicit def eitherStringToTryWithInternalErrorException[T](
      result: Either[String, T]
  ): Try[T] =
    result match {
      case Left(error) => Failure(new InternalErrorException(error))
      case Right(inst) => Success(inst)
    }

  implicit class StringEitherSyntax[R](either: Either[String, R]) {
    def toTryWithInternalErrorException(message: String): Try[R] = either match {
      case Left(error) => Failure(new InternalErrorException(s"$message | $error"))
      case Right(v)    => Success(v)
    }
  }
}
