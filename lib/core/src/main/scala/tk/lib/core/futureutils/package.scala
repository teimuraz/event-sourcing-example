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

import scala.concurrent.{ ExecutionContext, Future }

package object futureutils {

  /**
    * Run futures of individual slice in parallel, but next slice of future will
    * be run only when given slices completes.
    *
    * @param futures since in Scala Future is eager, it should be made lazy by wrapping it into function to avoid immediate execution
    * @param parallelismFactor numbers of futures to execute in parallel
    */
  def runParallellyAndSequentially[T](
      futures: Seq[() => Future[T]]
  )(
      implicit ec: ExecutionContext,
      parallelismFactor: ParallelismFactor
  ): Future[Seq[T]] = {

    val initial = Future.successful(Seq.empty[T])
    futures
      .grouped(parallelismFactor.parallelism)
      .foldLeft(initial) { (accFuture, currentSlice: Seq[() => Future[T]]) =>
        for {
          acc         <- accFuture
          sliceResult <- Future.sequence(currentSlice.map(item => item()))
        } yield {
          acc ++ sliceResult
        }
      }
  }

  /**
    * Run futures sequentially.
    * @param futures since in Scala Future is eager, it should be made lazy by wrapping it into function to avoid immediate execution
    */
  def runSequentially[T](futures: Seq[() => Future[T]])(
      implicit ec: ExecutionContext
  ): Future[Seq[T]] =
    runParallellyAndSequentially(futures)(ec, ParallelismFactor(1))
}
