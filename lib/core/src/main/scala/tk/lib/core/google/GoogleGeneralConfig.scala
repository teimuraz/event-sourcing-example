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

package tk.lib.core.google

import play.api.libs.json.{ Json, OFormat }

case class GoogleGeneralConfig(projectId: String)

object GoogleGeneralConfig {
  implicit lazy val format: OFormat[GoogleGeneralConfig] =
    Json.format[GoogleGeneralConfig]
}
