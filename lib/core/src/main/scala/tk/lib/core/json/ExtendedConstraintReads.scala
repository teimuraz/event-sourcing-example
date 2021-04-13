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

import play.api.libs.json.{ ConstraintReads, JsValue, JsonValidationError, Reads }
import play.api.libs.functional.syntax._
import tk.lib.core.datetime.DateTimeUtils
import tk.lib.core.datetime.DateTimeUtils

trait ExtendedConstraintReads extends ConstraintReads {

  /**
    * Validate that string is not empty ignoring whitespaces around it,
    * but resulted string will still be equal to original one (does not remove whites paces).
    */
  def notEmptyTrimmed: Reads[String] =
    filterNot[String](JsonValidationError("error.empty"))(_.trim.isEmpty)

  /**
    * Validate string's min length ignoring whitespaces around it,
    * but resulted string will still be equal to original one (does not remove whites paces).
    */
  def minLengthTrimmed(m: Int): Reads[String] =
    filterNot[String](JsonValidationError("error.minLength", m))(_.trim.length < m)

  /**
    * Validate string's max length ignoring whitespaces around it,
    * but resulted string will still be equal to original one (does not remove whites paces).
    */
  def maxLength(m: Int): Reads[String] =
    filterNot[String](JsonValidationError("error.maxLength", m))(_.trim.length > m)

  /**
    * Reads the string resulting in string with removed white spaces around it.
    */
  def trim: Reads[String] =
    (json: JsValue) => json.validate[String].map(_.trim)

  /**
    * Reads the string resulting in string with removed white spaces around it and than
    * validates for emptiness.
    */
  def trimAndNotEmpty: Reads[String] = trim keepAnd notEmptyTrimmed

  /**
    *  Reads the string resulting in string with removed white spaces around it and than
    *  validates for min length.
    */
  def trimAndMinLength(m: Int): Reads[String] = trim keepAnd minLengthTrimmed(m)

  /**
    *  Reads the string resulting in string with removed white spaces in the beginning and the end and than
    *  validates for max length.
    */
  def trimAndMaxLength(m: Int): Reads[String] = trim keepAnd maxLength(m)

  def date: Reads[String] =
    filter[String](JsonValidationError("error.date")) { value =>
      DateTimeUtils.parseInstantTried(value).isSuccess
    }

  def trimAndDate: Reads[String] = trim keepAnd date
}

object ExtendedConstraintReads extends ExtendedConstraintReads
