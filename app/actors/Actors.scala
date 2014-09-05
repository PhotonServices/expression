/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import play.api._
import play.api.libs.concurrent.Akka

object Actors {

  private def actors (implicit app: Application) = app.plugin[Actors]
      .getOrElse(sys.error("Actors plugin not registered"))

  def sentimentCardsManager (implicit app: Application) = actors.sentimentCardsManager

  def mediator (implicit app: Application) = actors.mediator
}

class Actors(implicit app: Application) extends Plugin {

  import akka.contrib.pattern.DistributedPubSubExtension

  /** The mediator actor which handles publishings and subscriptions. */
  private lazy val mediator = DistributedPubSubExtension(system).mediator

  /** */
  private lazy val sentimentCardsManager = system.actorOf(SentimentCardsManager.props(), "cards-manager")

  def system = Akka.system(app)
}
