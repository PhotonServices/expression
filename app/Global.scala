/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import play.api._
import actors.Actors

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Actors(app)
    Actors.sentimentCardsManager ! actors.Wake
  }

}
