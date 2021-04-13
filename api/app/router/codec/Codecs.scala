package router.codec

import tk.lib.core.datetime.DateTimeUtils
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.{ Codec, DecodeResult, Schema }
import java.time.Instant
import scala.util.{ Failure, Success }

trait Codecs extends SystemManagerCodecs with OrganizationCodes with UserCodecs {

  /// DateTime
  def dateTimeDecode(v: String): DecodeResult[Instant] =
    DateTimeUtils.parseInstantTried(v) match {
      case Failure(exception) => DecodeResult.Error(exception.getMessage, exception)
      case Success(value)     => DecodeResult.Value(value)
    }

  def dateTimeEncode(v: Instant): String = v.toString
  implicit lazy val dateTimeCodec: PlainCodec[Instant] =
    Codec.string.mapDecode(dateTimeDecode)(dateTimeEncode)
  implicit lazy val dateTimeSchema: Schema[Instant] = dateTimeCodec.schema

}

object Codecs extends Codecs
