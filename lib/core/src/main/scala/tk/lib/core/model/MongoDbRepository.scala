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

import reactivemongo.api.{ Cursor, ReadPreference }
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import tk.lib.core.tryutils.syntax._
import play.api.libs.json.{ __, Format, JsError, JsObject, JsSuccess, Json }
import reactivemongo.api.bson.{ BSONDocument, BSONObjectID }
import _root_.reactivemongo.api.bson._
import tk.lib.core.error.InternalErrorException
import reactivemongo.play.json.compat._
import json2bson.{ toDocumentReader, toDocumentWriter }

/**
  * Generic mongo db repository.
  */
trait MongoDbRepository[E, Id] {

  def components: MongoDbRepositoryComponents
  implicit def ec: ExecutionContext

  def collectionName: String
  implicit val idFormat: Format[Id]
  implicit val entityFormat: Format[E]
  implicit val entityLike: EntityLike[E, Id]

  def mongoIdFieldName: String = "_id"

  def collection: BSONCollection =
    components.database.collection(collectionName)

  /**
    * Find entity by id.
    */
  def find(id: Id): Future[Option[E]] = {
    val selector = Json.obj(mongoIdFieldName -> id)
    findBySelector(selector)
  }

  def findBySelector(selector: JsObject): Future[Option[E]] =
    collection.find(selector).one[JsObject](ReadPreference.primary).flatMap {
      docOpt: Option[JsObject] =>
        docOpt match {
          case None => Future.successful(None)
          case Some(doc) =>
            Future
              .fromTry(decodeDocument(doc))
              .map(Some(_))
        }
    }

  /**
    * Find all entities.
    */
  def findAll(): Future[Seq[E]] = {
    val selector = BSONDocument()
    findAllBySelector(selector)
  }

  /**
    * Find entities by ids. If entity with given id does not exist, it is ignored.
    */
  def findAllByIds(ids: Seq[Id]): Future[Seq[E]] =
    ids match {
      case Nil => Future.successful(Nil)
      case _ =>
        val selector =
          Json.obj(
            mongoIdFieldName -> Json.obj("$in" -> ids)
          )
        findAllBySelector(selector)
    }

  private def findAllBySelector(selector: JsObject): Future[Seq[E]] =
    for {
      docs <- collection
        .find(selector)
        .cursor[JsObject](ReadPreference.primary)
        .collect[Seq](-1, Cursor.FailOnError[Seq[JsObject]]())
      entities <- Future.fromTry(Try.fromSeqOfTries(docs.map(decodeDocument)))
    } yield {
      entities
    }

  /**
    * Store the entity with optimistic lock.
    */
  def store(entity: E): Future[E] = {
    val entityId      = entityLike.id(entity)
    val metadata      = entityLike.metadata(entity)
    val entityVersion = metadata.version
    val selector =
      Json.obj(
        mongoIdFieldName   -> entityId,
        "metadata.version" -> entityVersion
      )

    val newVersion = entityVersion + 1

    val updatedEntity = entityLike.copyWithMetadata(
      entity,
      metadata.copy(version = newVersion)
    )

    val document = encodeEntity(updatedEntity)

    for {
      // Save with optimistic lock:
      // Try to find the entity with its current version and update the state and increase the version (by 1) (atomically)
      // If entity with its current version doesn't exist that means that either the entity itself doesn't exist
      // (in this case it will try to create new entity (upsert = true)) or it has been modified concurrently
      // by another process.
      _ <- collection.update
        .one(q = selector, u = document, upsert = true, multi = false)
        .recoverWith {
          case e if e.getMessage.contains("E11000 duplicate key error collection") =>
            // Mongodb did not find document with entity's id and entity's current version and therefore tried to
            // create new doc with given id (upsert=true), but since upsert failed with duplicate key
            // violation (id is primary key) that means that entity with given id actually exists but with
            // different version.
            // So indeed, the entity has been modified concurrently by another process.
            // Raise ConcurrencyException.
            Future.failed(
              new ConcurrencyException(
                s"Entity with id $entityId has been " +
                s"modified concurrently by another process.",
                Some(e)
              )
            )
        }
    } yield {
      updatedEntity
    }
  }

  private def encodeEntity(entity: E): JsObject = {
    val entityId = entityLike.id(entity)
    Json
      .toJson(entity)
      .as[JsObject]
      // set "_id" (or field name which represents mongodb id field (mongoIdField) to entity's id value
      .+(mongoIdFieldName, Json.toJson(entityId))
      // remove "id" from json
      .-("id")
  }

  private def decodeDocument(doc: JsObject): Try[E] = {
    // Rename "_id"  (or field name which represents mongodb id field (mongoIdField)) to id in json
    val transformer = {
      __.json.update(
        (__ \ "id").json
          .copyFrom((__ \ mongoIdFieldName).json.pick)
          .andThen((__ \ mongoIdFieldName).json.prune)
      )
    }
    val result = for {
      docWithRenamedId <- doc.transform(transformer)
      entity           <- docWithRenamedId.validate[E]
    } yield {
      entity
    }

    result match {
      case JsError(errors) =>
        Failure(
          new MongoDbRepositoryException(
            s"Failed to decode document. Document $doc, errors: $errors"
          )
        )
      case JsSuccess(value, _) => Success(value)
    }
  }

  /**
    * Delete the entity.
    */
  def delete(id: Id): Future[Id] =
    collection
      .delete()
      .one(Json.obj(mongoIdFieldName -> id))
      .map(_ => id)

  def nextObjectId(): Future[String] =
    Future.successful(BSONObjectID.generate().stringify)
}
