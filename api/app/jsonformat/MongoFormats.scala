package jsonformat

import tk.lib.eventsourcing.EventStore
import tk.lib.eventsourcing.EventStore.EventStoreRecordMongoDbFormat
import play.api.libs.json.Format

object MongoFormats extends Formats with tk.lib.core.mongo.json.Formats {

  override implicit def eventStoreRecordDefaultFormat[Id: Format, Evt: Format]
      : Format[EventStore.EventStoreRecord[Id, Evt]] =
    EventStoreRecordMongoDbFormat.format
}
