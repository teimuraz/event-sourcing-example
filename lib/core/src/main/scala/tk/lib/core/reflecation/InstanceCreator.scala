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

package tk.lib.core.reflecation

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

trait InstanceCreator {

  def create[T: TypeTag](args: AnyRef*)(ctor: Int = 0): T = {
    val tt = typeTag[T]
    currentMirror
      .reflectClass(tt.tpe.typeSymbol.asClass)
      .reflectConstructor(
        tt.tpe.members
          .filter(m => m.isMethod && m.asMethod.isConstructor)
          .iterator
          .toSeq(ctor)
          .asMethod
      )(args: _*)
      .asInstanceOf[T]
  }
}

object InstanceCreator extends InstanceCreator
