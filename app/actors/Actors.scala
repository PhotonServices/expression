/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import play.api._
import play.api.libs.concurrent.Akka

object Actors {

  private var app: Application = null

  private def actors = app.plugin[Actors].getOrElse(sys.error("Actors plugin not registered"))

  def apply (_app: Application): Unit = if (app == null) app = _app

  def sentimentCardsManager = actors.sentimentCardsManager

  def mediator = actors.mediator
}

class Actors(app: Application) extends Plugin {

  import akka.contrib.pattern.DistributedPubSubExtension

  /** The mediator actor which handles publishings and subscriptions. */
  private lazy val mediator = DistributedPubSubExtension(system).mediator

  /** */
  private lazy val sentimentCardsManager = system.actorOf(SentimentCardsManager.props(), "cards-manager")

  def system = Akka.system(app)
}
