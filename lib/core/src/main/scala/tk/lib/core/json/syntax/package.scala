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

package tk.lib.core.json

import tk.lib.core.validation.Validation.ValidationResult
import play.api.libs.json.{ __, JsError, JsResult, JsSuccess, Json, OWrites, Writes }
import play.api.libs.functional.syntax._

package object syntax {
  implicit def eitherStringToJsResult[T](
      result: Either[String, T]
  ): JsResult[T] =
    result match {
      case Left(error) => JsError(error)
      case Right(inst) => JsSuccess(inst)
    }

  class ExtendedWrites[A](writes: OWrites[A]) {
    def addField[T: Writes](fieldName: String, field: A => T): OWrites[A] =
      (writes ~ (__ \ fieldName).write[T])((a: A) => (a, field(a)))

    def removeField(fieldName: String): OWrites[A] = OWrites { a: A =>
      val transformer = (__ \ fieldName).json.prune
      Json.toJson(a)(writes).validate(transformer).get
    }
  }

  implicit def toExtendedWrites[A](writes: OWrites[A]): ExtendedWrites[A] =
    new ExtendedWrites(writes)

  implicit def validationResultToJsResult[T](
      validationResult: ValidationResult[T]
  ): JsResult[T] =
    validationResult match {
      case Left(validationError) => JsError(validationError.message)
      case Right(inst)           => JsSuccess(inst)
    }
}
