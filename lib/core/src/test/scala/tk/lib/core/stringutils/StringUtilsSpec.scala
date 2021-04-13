/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.core.stringutils

import tk.lib.core.stringutils.Implicits._
import tk.lib.core.testutil.SpecLike
import org.scalatest.freespec.AnyFreeSpec

class StringUtilsSpec extends AnyFreeSpec with SpecLike {

  "slugify" - {
    "when value is 'Hello World'" - {
      "should generate 'hello-world'" in {
        val slug = "Hello World".slugify
        slug should be("hello-world")
      }
    }

    "when value is 'Hello       World'" - {
      "should generate 'hello-------world'" in {
        val slug = "Hello       World".slugify
        slug should be("hello-------world")
      }
    }

    "when value is empty" - {
      "should return empty string" in {
        val slug = "".slugify
        slug should be("")
      }
    }
  }

  "fullTrim" - {
    "when value is 'Hello     World'" - {
      "should replace more then 2 spaces in string with 1 space and generate 'Hello World" in {
        "Hello    World".fullTrim should be("Hello World")
      }
    }
  }
}
