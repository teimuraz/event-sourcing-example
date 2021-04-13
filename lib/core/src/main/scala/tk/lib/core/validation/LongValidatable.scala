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

import tk.lib.core.validation.Validation.ValidationFunc

trait LongValidatable[T] extends Validatable[Long, T] {
  def min: Option[(Long, ValidationMessage)] = None

  def minValidation: Option[ValidationFunc[Long]] =
    min.map {
      case (minValue, msg) =>
        val message = msg match {
          case DefaultMessage =>
            s"${this.getClass.getSimpleName.replace("$", "")} must be greater or equal to $minValue"
          case Custom(m) => m
        }

        v: Long => (v >= minValue, message)
    }

  override def validations: Seq[ValidationFunc[Long]] =
    Seq(
      minValidation
    ).flatten

}
