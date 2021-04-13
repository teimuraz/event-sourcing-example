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

package tk.lib.core.model

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait EntityLike[E, Id] {
  def id(e: E): Id
  def metadata(e: E): Metadata
  def copyWithMetadata(e: E, newMetadata: Metadata): E
}

object EntityLike {

  def auto[E, Id]: EntityLike[E, Id] = macro autoImpl[E, Id]

  def autoImpl[E: c.WeakTypeTag, Id: c.WeakTypeTag](c: Context) = {
    import c.universe._
    val entityType   = weakTypeOf[E]
    val idType       = weakTypeOf[Id]
    val metadataType = weakTypeOf[Metadata]

    q"""
          new EntityLike[$entityType, $idType] {
            override def id(e: $entityType): $idType                 = e.id
            override def metadata(e: $entityType): $metadataType         = e.metadata
            override def copyWithMetadata(e: $entityType, newMetadata: $metadataType): $entityType = 
              e.copy(metadata = newMetadata)
          }
     """
  }
}
