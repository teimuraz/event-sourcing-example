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

package tk.lib.core.config

import com.typesafe.config.{ ConfigFactory, ConfigRenderOptions }
import play.api.Logging
import play.api.libs.json.{ JsError, JsSuccess, Json, Reads }

import scala.util.{ Failure, Success, Try }

trait ConfigBuilder extends Logging {

  def build[T: Reads](path: String): Try[T] = {
    val configJsonString =
      ConfigFactory
        .defaultApplication()
        .resolve()
        .getObject(path)
        .render(ConfigRenderOptions.concise())
        // Make numeric string numbers, otherwise ApplicationConfig int properties wan't be parsed
        .replaceAll(":\"(\\d+)\"", ":$1")

    val configJson = Json.parse(configJsonString)

    configJson.validate[T] match {
      case JsSuccess(value, _) => Success(value)
      case e: JsError =>
        val message = s"Cannot load config. Error: ${e.errors}"
        logger.error(message)
        Failure(new ConfigurationException(message))
    }
  }
}

object ConfigBuilder extends ConfigBuilder
