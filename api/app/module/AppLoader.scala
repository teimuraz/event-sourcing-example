package module

import play.api.{ Application, ApplicationLoader }

class AppLoader extends ApplicationLoader {
  override def load(context: ApplicationLoader.Context): Application =
    new Components(context).application
}
