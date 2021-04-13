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

object Validation {

  def must(
      requirement: Boolean,
      message: String = "Requirement failed"
  ): Option[ValidationError] =
    if (!requirement) Some(ValidationError(message)) else None

  type ValidationFunc[V] = V => (Boolean, String)

  type ValidationResult[T] = Either[ValidationError, T]
}
