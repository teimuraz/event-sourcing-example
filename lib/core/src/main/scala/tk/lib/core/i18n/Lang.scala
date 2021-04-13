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

package tk.lib.core.i18n

import java.util.Locale

import play.api.libs.json.{ Format, JsResult, JsString, JsValue }

case class Lang(locale: Locale)

object Lang {
  implicit val format: Format[Lang] = new Format[Lang] {
    override def reads(json: JsValue): JsResult[Lang] =
      json.validate[String].map(l => Lang(new Locale(l)))

    override def writes(o: Lang): JsValue = JsString(o.locale.getLanguage)
  }

  val EN_US: Lang = Lang(Locale.US)

  val default: Lang = EN_US
}
