# Event Sourcing Example

This is a fork from my private project.
Only some basic models are left to demonstrate an event sourcing implementation.

MongoDB is used for event storage.

Events from event store are published to Google PubSub topics, so they can be consumed to 
build projections, trigger some other business processes, etc.

Play framework is used as http layer.

Default play's router is replaced with [Tapir](https://tapir.softwaremill.com), so 
we can take an advantage of generating an open api documentation in type-safe way.

The app uses Firebase for authentication.


### Running application

The setup is bit involved, since it requires google cloud and firebase projects setup.


It could be made simpler for demo, but this is just quick fork from the main project.

#### Requirements

- Google Cloud Project
- Firebase Project
- Service account key with `owner` permission (should be more restrictive in production environment)
  (service account key json should be downloaded and GOOGLE_APPLICATION_CREDENTIALS environment variable should point to this file)
- Running MongoDB instance (> 4.4) with replica set


##### Run
```
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service/account/json/key
export MONGODB_URI=mongodb://localhost:27017/tk # Replace with your credentials
export PROJECT_ID=... # Google Project Id
sbt "api/run"
```

Navigate to automatically generated [swagger ui](http://localhost:9000/docs) to play with api.

To obtain firebase tokens, you can fork and run [firebase token generator](https://github.com/teimuraz/firebase-token-gen) (replace firebase credentials with your own).
