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

package tk.lib.core

import java.util.UUID

import scala.util.Try

/**
  * Facade around java.util.UUID so it can be injected / mocked.
  */
trait UUIDUtils {
  def randomUUID: UUID = UUID.randomUUID()

  def parse(value: String): Try[UUID] = Try(UUID.fromString(value))
}

object UUIDUtils extends UUIDUtils
