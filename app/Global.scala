/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import play.api._
import actors.Actors
import akka.contrib.pattern.DistributedPubSubMediator.Put

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    println(Actors.sentimentCardsManager(app).path)
    Actors.mediator(app) ! Put(Actors.sentimentCardsManager(app))
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}
