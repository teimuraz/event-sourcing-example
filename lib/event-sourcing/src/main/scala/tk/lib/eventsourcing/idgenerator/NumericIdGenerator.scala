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

package tk.lib.eventsourcing.idgenerator

import scala.reflect.runtime._
import scala.reflect.runtime.universe._
import scala.concurrent.Future

class NumericIdGenerator {
  def generate[Id: TypeTag](): Future[Id] = {
    // TODO:: temporary
    val value = System.currentTimeMillis()
    val id    = createIdInstance[Id](value)
    Future.successful(id)
  }

  def createIdInstance[T: TypeTag](value: Long): T = {
    val tt = typeTag[T]

    currentMirror
      .reflectClass(tt.tpe.typeSymbol.asClass)
      .reflectConstructor(
        tt.tpe.members
          .filter(m => m.isMethod && m.asMethod.isConstructor)
          .iterator
          .next
          .asMethod
      )(value)
      .asInstanceOf[T]
  }
}
