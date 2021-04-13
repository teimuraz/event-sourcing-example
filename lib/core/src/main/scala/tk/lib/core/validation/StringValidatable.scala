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

import tk.lib.core.stringutils.Implicits.StringOps
import tk.lib.core.validation.Validation.ValidationFunc
import tk.lib.core.stringutils._

trait StringValidatable[T] extends Validatable[String, T] {
  def notEmpty: Option[ValidationMessage]         = None
  def notEmptyProp: Option[ValidationMessage]     = None
  def minLength: Option[(Int, ValidationMessage)] = None
  def maxLength: Option[(Int, ValidationMessage)] = None

  /**
    * Trim string before validation.
    * Value will be constructed with full trimmed value.
    * @return
    */
  def trim: Boolean = true

  /**
    * Full trim string (trim string and also remove extra white space characters from the middle string).
    * Value will be constructed with full trimmed value.
    * #trim overwrites this value, e.g. if #trim == False and #fullTrim == True, then #fullTrim = False
    * @return
    */
  def fullTrim: Boolean = true

  def notEmptyValidation: Option[ValidationFunc[String]] = notEmpty.map { msg =>
    val message = msg match {
      case DefaultMessage =>
        s"${this.getClass.getSimpleName.replace("$", "")} must not be empty"
      case Custom(m) => m
    }
    v: String => (!v.isEmpty, message)
  }

  def minLengthValidation: Option[ValidationFunc[String]] =
    minLength.map {
      case (lnt, msg) =>
        val message = msg match {
          case DefaultMessage =>
            s"${this.getClass.getSimpleName.replace("$", "")} must contain at lest $lnt characters"
          case Custom(m) => m
        }

        v: String => (v.length >= lnt, message)
    }

  def maxLengthValidation: Option[ValidationFunc[String]] =
    maxLength.map {
      case (lnt, msg) =>
        val message = msg match {
          case DefaultMessage =>
            s"${this.getClass.getSimpleName.replace("$", "")} must contain at most $lnt characters"
          case Custom(m) => m
        }
        v: String => (v.length <= lnt, message)
    }

  override def validations: Seq[ValidationFunc[String]] =
    Seq(
      notEmptyValidation,
      minLengthValidation,
      maxLengthValidation
    ).flatten

  override def normalize(v: String): String = {
    val makeFullTrim = if (!trim) false else fullTrim
    if (makeFullTrim) {
      v.fullTrim
    } else if (trim) {
      v.trim
    } else {
      v
    }
  }
}
