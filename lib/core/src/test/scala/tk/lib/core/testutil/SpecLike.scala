/*
 * Copyright 2021 TK
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package tk.lib.core.testutil

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ EitherValues, TryValues }
import org.scalatestplus.mockito.MockitoSugar
import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, Future }

trait SpecLike
    extends Matchers
    with ScalaFutures
    with TryValues
    with EitherValues
    with MockitoSugar {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = 300.seconds
  )

  def awaitResult[T](future: Future[T]): T =
    Await.result(future, 300.seconds)

  def dateTime(d: String): Instant = Instant.parse(d)
}
