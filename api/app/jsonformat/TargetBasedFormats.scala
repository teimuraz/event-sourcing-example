package jsonformat

import tk.lib.eventsourcing.EventStore.EventStoreRecord
import play.api.libs.json.Format

import java.time.Instant

trait TargetBasedFormats {
  implicit def instantFormat: Format[Instant]

  implicit def eventStoreRecordDefaultFormat[Id: Format, Evt: Format]
      : Format[EventStoreRecord[Id, Evt]]
}
