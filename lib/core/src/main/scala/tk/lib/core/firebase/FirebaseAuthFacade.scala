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

package tk.lib.core.firebase

import tk.lib.core.concurrent.BlockingIoExecutionContext
import com.google.firebase.auth.{ FirebaseAuth, UserRecord }
import scala.concurrent.Future
import java.util
import com.google.common.collect.ImmutableMap

class FirebaseAuthFacade(firebaseAuth: FirebaseAuth)(
    implicit ec: BlockingIoExecutionContext
) {

  def getUser(id: String): Future[Option[UserRecord]] =
    Future {
      val userRecord: UserRecord = firebaseAuth.getUser(id)
      Option(userRecord)
    }(ec.underlying)

  def setCustomUserClaims(
      firebaseUID: String,
      claims: util.Map[String, AnyRef]
  ): Future[String] =
    Future {
      firebaseAuth.setCustomUserClaims(firebaseUID, claims)
      firebaseUID
    }(ec.underlying)

  def addCustomUserClaim(user: UserRecord, key: String, value: AnyRef): Future[String] = {
    val oldClaims: util.Map[String, AnyRef] = user.getCustomClaims
    val copy: util.Map[String, AnyRef] =
      new util.HashMap[String, AnyRef](oldClaims)
    copy.put(key, value)
    val newClaims: ImmutableMap[String, AnyRef] = ImmutableMap.copyOf(copy)
    setCustomUserClaims(user.getUid, newClaims)
  }

  def createUser(email: String, password: String): Future[UserRecord] =
    Future {
      val req = new UserRecord.CreateRequest
      req.setEmail(email)
      req.setPassword(password)
      firebaseAuth.createUser(req)
    }(ec.underlying)

  def updateUserDisplayName(
      firebaseUID: String,
      displayName: String
  ): Future[UserRecord] =
    Future {
      val req = new UserRecord.UpdateRequest(firebaseUID)
      req.setDisplayName(displayName)
      firebaseAuth.updateUser(req)
    }(ec.underlying)

}
