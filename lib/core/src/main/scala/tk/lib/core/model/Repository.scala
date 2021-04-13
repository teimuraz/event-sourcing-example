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

import scala.concurrent.Future

trait Repository[E, Id] {

  /**
    * Find entity by id.
    */
  def find(id: Id): Future[Option[E]]

  /**
    * Find all entities.
    */
  def findAll(): Future[Seq[E]]

  /**
    * Find entities by ids. If entity with given id does not exist, it is ignored.
    */
  def findAllByIds(ids: Seq[Id]): Future[Seq[E]]

  /**
    * Store the entity.
    */
  def store(entity: E): Future[E]

  /**
    * Delete the entity.
    */
  def delete(id: Id): Future[Id]
}
