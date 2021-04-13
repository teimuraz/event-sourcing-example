/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.core.validation

import tk.lib.core.testutil.BaseSpec
import tk.lib.core.validation.Validation._
import tk.lib.core.validation.ValidationError

class ValidationSpec extends BaseSpec {

  case class UserTest(userId: TestUserId)

  "must" - {
    "when requirement is not satisfied" - {
      "should throw exception" in {
        val result = must(1 == 2, "1 not equals 2")
        result should be(Some(ValidationError("1 not equals 2")))
      }
    }
    "when requirement is satisfied" - {
      "should do nothing" in {
        val r = must(1 == 1)
        r should be(None)
      }
    }
  }

  "Validatable" - {
    "when validatable to value class with Long type applied, and value should not equal 0 rule set" - {
      "should fail" in {
        val result = TestUserId(0)
        result.left.value should be(ValidationError("Value must not be 0"))
      }
    }
    "when Validatable to value class with Long type applied, and value should be less than 100 rule set" - {
      "should fail" in {
        val result = TestUserId(101)
        result.left.value should be(ValidationError("Value should be less then 100"))
      }
    }
    "when Validation rules set and valid value is provided" - {
      "should create value" in {
        val r = TestUserId(99)
        r.right.value.value should be(99)
      }
    }
    "when creating without validation " - {
      "should create value in incorrect state without failing" in {
        TestUserId.notValidated(0).value should be(0)
      }
    }
  }

  "LongValidatable" - {
    "when min rule set and value less than min provided" - {
      "should fail" in {
        val result = TestLong(-1)
        result.left.value should be(
          ValidationError("TestLong must be greater or equal to 0")
        )
      }
    }
    "when min rule set and value greater than or equal to min provided" - {
      "should crate value" in {
        val result = TestLong(1)
        result.right.value.value should be(1)
      }
    }
  }

  "StringValidatable" - {
    "when not empty rule set and empty string provided" - {
      "should fail" in {
        val result = TestName("")
        result.left.value should be(ValidationError("Name must not be empty"))
      }
    }
    "when not empty rule set and string with white spaces provided" - {
      "should fail" in {
        val result = TestName("   ")
        result.left.value should be(ValidationError("Name must not be empty"))
      }
    }
    "when min length rule set and string with less then required length provided" - {
      "should fail" in {
        val result = TestName("12")
        result.left.value should be(
          ValidationError("Name must contain at least 6 characters")
        )
      }
    }
    "when min length rule set and string with more then required length provided but with empty spaces " +
    "(2 or more following each other) in the middle, so that its actual length is less then required" - {
      "should mail" in {
        val result = TestName("1        2")
        result.left.value should be(
          ValidationError("Name must contain at least 6 characters")
        )
      }
    }
    "when min length rule set and string with more then required length provided but with empty spaces at edges, so that " +
    "its actual length is less then required" - {
      "should fail" in {
        val result = TestName("    12         ")
        result.left.value should be(
          ValidationError("Name must contain at least 6 characters")
        )
      }
    }
    "when max length rule set and string with more then required length provided" - {
      "should fail" in {
        val result = TestName("12345678901")
        result.left.value should be(
          ValidationError("Name must contain at most 10 characters")
        )
      }
    }
    "when max length rule set and string with more then required length provided but with empty spaces both at edges and at" +
    " the middle (2 or more following each other), so that its actual length is passes requirement" - {
      "should create value with trimmed spaces" in {
        val v = TestName("   123456   789      ")
        v.right.value.value should be("123456 789")
      }
    }
    "when max length rule set and string with satisfied length provided" - {
      "should create value" in {
        val v = TestName("1234567")
        v.right.value.value should be("1234567")
      }
    }
    "when adding additional rule" - {
      "should check this additional rule" in {
        val result = TestName("NotValid")
        result.left.value should be(ValidationError("Not valid value provided"))
      }
    }
    "when value is incorrect and default message option set" - {
      "should fail with default message" in {
        val result = AnotherTestName("")
        result.left.value should be(ValidationError("AnotherTestName must not be empty"))
      }
    }
    "when value with spaces at edges and extra spaces provided and fullTrim flag set to False" - {
      "should trim only at edges leaving extra spaces in the middle" in {
        val name = NameWithTrimFlagOnly("   he   llo     ")
        name.right.value.value should be("he   llo")
      }
    }
    "when value with spaces at edges and extra spaces provided and trim flag set to False, but fullTrim flag set to True" - {
      "should trim flag should overwrite fullTrim flag (e.g., make it false) and value should not be trimmed at all" in {
        val name =
          NameWithFullTrimmedButWithoutTrimFlag("   he   llo     ")
        name.right.value.value should be("   he   llo     ")
      }
    }
  }

  "EmailValidatable" - {
    "when incorrect email format is provided" - {
      "should fail" in {
        val result = TestEmail("ddfkjkj")
        result.left.value should be(
          ValidationError("TestEmail must be correct email address")
        )
      }
    }
    "when incorrect email format is provided and custom validation message is set" - {
      "should fail with custom message" in {
        val result = TestEmailWithCustomMessage("ddfkjkj")
        result.left.value should be(
          ValidationError("Some custom message which states that email is incorrect")
        )
      }
    }
    "when correct email format is provided" - {
      "should create value with that email" in {
        val email = TestEmail("teimuraz.kantaria@gmail.com")
        email.right.value.value should be("teimuraz.kantaria@gmail.com")
      }
    }
    "when correct email format is provided but with spaces at edges" - {
      "should create value with trimmed email" in {
        val email = TestEmail("   teimuraz.kantaria@gmail.com    ")
        email.right.value.value should be("teimuraz.kantaria@gmail.com")
      }
    }
  }
}

sealed abstract case class TestUserId private (value: Long)

object TestUserId extends Validatable[Long, TestUserId] {
  override def validations: Seq[ValidationFunc[Long]] = Seq(
    { v: Long =>
      (v != 0, "Value must not be 0")
    }, { v: Long =>
      (v < 100, "Value should be less then 100")
    }
  )

  override def inst: Long => TestUserId = new TestUserId(_) {}
}

sealed abstract case class TestLong private (value: Long)

object TestLong extends LongValidatable[TestLong] {
  override def min: Option[(Long, ValidationMessage)] =
    Some((0, DefaultMessage))
  override def inst: Long => TestLong = new TestLong(_) {}
}

sealed abstract case class TestName private (value: String)

object TestName extends StringValidatable[TestName] {
  override def notEmpty: Option[Custom] = Some(Custom("Name must not be empty"))
  override def minLength: Option[(Int, Custom)] =
    Some((6, Custom("Name must contain at least 6 characters")))
  override def maxLength: Option[(Int, Custom)] =
    Some((10, Custom("Name must contain at most 10 characters")))
  override def inst: String => TestName = new TestName(_) {}

  override def validations: Seq[ValidationFunc[String]] =
    super.validations ++ Seq(
      { v: String =>
        (v != "NotValid", "Not valid value provided")
      }
    )
}

sealed abstract case class AnotherTestName private (value: String)

object AnotherTestName extends StringValidatable[AnotherTestName] {
  override def notEmpty: Option[ValidationMessage] = Some(DefaultMessage)
  override def inst: String => AnotherTestName     = new AnotherTestName(_) {}
}

sealed abstract case class NameWithTrimFlagOnly private (value: String)

object NameWithTrimFlagOnly extends StringValidatable[NameWithTrimFlagOnly] {
  override def fullTrim: Boolean                   = false
  override def notEmpty: Option[ValidationMessage] = Some(DefaultMessage)
  override def inst: String => NameWithTrimFlagOnly =
    new NameWithTrimFlagOnly(_) {}
}

sealed abstract case class NameWithFullTrimmedButWithoutTrimFlag private (value: String)

object NameWithFullTrimmedButWithoutTrimFlag
    extends StringValidatable[NameWithFullTrimmedButWithoutTrimFlag] {
  override def trim: Boolean                       = false
  override def notEmpty: Option[ValidationMessage] = Some(DefaultMessage)
  override def inst: String => NameWithFullTrimmedButWithoutTrimFlag =
    new NameWithFullTrimmedButWithoutTrimFlag(_) {}
}

sealed abstract case class TestEmail private (value: String)

object TestEmail extends EmailValidatable[TestEmail] {
  override def inst = new TestEmail(_) {}
}

sealed abstract case class TestEmailWithCustomMessage private (value: String)

object TestEmailWithCustomMessage extends EmailValidatable[TestEmailWithCustomMessage] {
  override def validEmail: ValidationMessage =
    Custom("Some custom message which states that email is incorrect")
  override def inst = new TestEmailWithCustomMessage(_) {}
}
