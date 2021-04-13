package router.codec

import tk.lib.core.auth.api.UserId
import command.SignUpCommand.{ SignUpRequest, SignUpResult }
import domain.user.UserState
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.{ Codec, Schema }

trait UserCodecs {
  /// UserId
  implicit lazy val userIdCodec: PlainCodec[UserId] =
    Codec.long
      .map(UserId(_))(_.value)
  implicit lazy val userIdSchema: Schema[UserId] = userIdCodec.schema

  /// UserState
  implicit lazy val userStateSchema: Schema[UserState] =
    Schema.derived

  implicit lazy val signupRequestSchema: Schema[SignUpRequest] = Schema.derived

  implicit lazy val signUpResultSchema: Schema[SignUpResult] = Schema.derived

}
