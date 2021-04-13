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

package tk.lib.core.crypto

import java.security.MessageDigest

import javax.xml.bind.DatatypeConverter

object Crypto {
  def md5(value: String): String = {
    val md = MessageDigest.getInstance("MD5")
    md.update(value.getBytes)
    DatatypeConverter.printHexBinary(md.digest).toUpperCase()
  }
}
