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

package tk.lib.core.auth.api

trait Context[+T <: Identity] {
  def identity: T
  def ip: Option[String]
}

object Context {
  type AnyContext = Context[Identity]
}

case class GuestContext(
    ip: Option[String] = None
) extends Context[GuestIdentity.type] {
  override def identity: GuestIdentity.type = GuestIdentity
}

case class AuthenticatedContext(
    identity: AuthenticatedIdentity,
    ip: Option[String] = None
) extends Context[AuthenticatedIdentity]
