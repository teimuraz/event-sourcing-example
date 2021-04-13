package jsonformat

import tk.lib.eventsourcing.EventStore
import tk.lib.eventsourcing.EventStore.EventStoreRecordDefaultFormat
import play.api.libs.json.Format

object DefaultFormats extends Formats with tk.lib.core.json.Formats {

  override implicit def eventStoreRecordDefaultFormat[Id: Format, Evt: Format]
      : Format[EventStore.EventStoreRecord[Id, Evt]] =
    EventStoreRecordDefaultFormat.format
}
