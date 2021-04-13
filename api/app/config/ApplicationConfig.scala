package config

import tk.lib.eventsourcing.EventStorePubsubConnector
import play.api.libs.json.{ Json, OFormat }

case class ApplicationConfig(
    eventStorePubsubConnector: EventStorePubsubConnector.Config,
    google: GoogleConfig
)

object ApplicationConfig {
  implicit val format: OFormat[ApplicationConfig] = Json.format[ApplicationConfig]
}
