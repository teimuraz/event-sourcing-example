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

package tk.lib.core.datetime

import java.time.{ Instant, ZoneId }
import java.time.format.DateTimeFormatter
import scala.util.Try

trait DateTimeUtils {
  def now: Instant = Instant.now()

  def parseInstantTried(
      dateTime: String
  ): Try[Instant] =
    Try {
      Instant
        .parse(dateTime)
    }

  def formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))

  def instantToString(instant: Instant): String = formatter.format(instant)
}

object DateTimeUtils extends DateTimeUtils
