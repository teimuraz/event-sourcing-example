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

import tk.lib.core.validation.Validation.{ must, ValidationFunc, ValidationResult }

trait Validatable[V, T] {

  def validations: Seq[ValidationFunc[V]]

  // Don't allow "direct" creation of objects

  def notValidated(value: V): T = {
    val normalized = normalize(value)
    inst(normalized)
  }

  def apply(value: V): ValidationResult[T] = {
    val normalized = normalize(value)

    var firstErrorMessage: Option[String] = None

    // TODO:: improve in more functional way`
    validations.dropWhile { validate =>
      val (result, msg) = validate(normalized)
      val isDefined     = must(result, msg).isDefined
      if (isDefined) {
        firstErrorMessage = Some(msg)
      }
      !isDefined
    }.lastOption match {
      case Some(_) =>
        if (firstErrorMessage.isEmpty) {
          println(validations)
        }
        Left(ValidationError(firstErrorMessage.get))
      case None => Right(inst(normalized))
    }
  }

  def inst: V => T

  def normalize(v: V): V = v
}
