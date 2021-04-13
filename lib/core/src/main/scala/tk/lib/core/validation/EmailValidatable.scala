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

trait EmailValidatable[T] extends StringValidatable[T] {
  def validEmail: ValidationMessage                = DefaultMessage
  override def notEmpty: Option[ValidationMessage] = Some(DefaultMessage)
  override def fullTrim: Boolean                   = false
  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def correctEmail: ValidationFunc[String] = {
    val message = validEmail match {
      case DefaultMessage =>
        s"${this.getClass.getSimpleName.replace("$", "")} must be correct email address"
      case Custom(m) => m
    }
    v: String => (emailRegex.findFirstMatchIn(v).isDefined, message)
  }

  override def validations: Seq[ValidationFunc[String]] =
    super.validations :+ correctEmail
}
