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

package tk.lib.eventsourcing

trait EntityBehaviour {

  type Id
  type S <: State
  type Evt <: Event[Id]

  trait State {
    def id: Id
    def applyEvent(event: Evt): S
  }

  val emptyState: S

  case class Entity(state: S, metadata: Metadata[Evt]) {
    def applyChange(event: Evt): Entity = {
      val updatedState = state.applyEvent(event)
      val updatedMetadata = metadata.copy(
        uncommittedChanges = metadata.uncommittedChanges :+ event,
        currentVersion = metadata.currentVersion + 1
      )
      copy(updatedState, updatedMetadata)
    }
  }

  def emptyEntity: Entity = Entity(emptyState, Metadata.empty[Evt])

  def withState[T](entity: Entity)(f: S => T): T =
    f(entity.state)
}
