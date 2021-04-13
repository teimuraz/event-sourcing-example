package config

import tk.lib.core.google.GoogleGeneralConfig
import config.GoogleConfig.PubsubConfig
import play.api.libs.json.{ Json, OFormat }

case class GoogleConfig(general: GoogleGeneralConfig, pubsub: PubsubConfig)

object GoogleConfig {
  implicit val format: OFormat[GoogleConfig] = Json.format[GoogleConfig]

  case class PubsubConfig(
      userEventsTopic: String,
      userEventsTopic_userProjectionsSubscription: String,
      systemManagerEventsTopic: String,
      systemManagerEventsTopic_userProjectionsSubscription: String,
      organizationEventsTopic: String,
      organizationEventsTopic_organizationProjectionsSubscription: String,
      organizationEventsTopic_userProjectionsSubscription: String
  )

  object PubsubConfig {
    implicit val format: OFormat[PubsubConfig] = Json.format[PubsubConfig]
  }
}
