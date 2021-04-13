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

package tk.lib.core

import _root_.play.api.libs.json.JsValue

package object error {

  class BaseException(message: String, cause: Option[Throwable] = None)
      extends RuntimeException(message, cause.orNull)

  class InternalErrorException(message: String, cause: Option[Throwable] = None)
      extends BaseException(message, cause = cause) {
    def this(cause: Throwable) {
      this(cause.getMessage, Some(cause))
    }
  }

  class RethrownFromTrackedStackException(cause: Throwable)
      extends BaseException(cause.getMessage, Some(cause))

  class UnauthorizedException(message: String) extends BaseException(message)

  class ForbiddenException(message: String) extends BaseException(message)

  class ValidationException(message: String, cause: Option[Throwable] = None)
      extends BaseException(message, cause)

  class NotFoundException(message: String) extends BaseException(message)

  class ConfigurationException(message: String, cause: Option[Throwable] = None)
      extends InternalErrorException(message, cause)

  class InvalidJsonException(val message: String, val errors: JsValue)
      extends ValidationException(message)

}
