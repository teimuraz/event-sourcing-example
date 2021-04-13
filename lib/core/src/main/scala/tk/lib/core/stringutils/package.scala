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

package object stringutils {
  object Implicits {
    implicit class StringOps(str: String) {

      /**
        * Trim white spaces in the beginning and the end, also leave only single white spaces in string.
        *
        * @return
        */
      def fullTrim: String = str.replaceAll("\\s{2,}", " ").trim

      def slugify: String = {
        import java.text.Normalizer
        Normalizer
          .normalize(str, Normalizer.Form.NFD)
          .replaceAll("[^\\w ]", "")
          .replace(" ", "-")
          .toLowerCase
      }
    }
  }
}
