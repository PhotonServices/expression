/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor.ActorSystem

/** Gives access to global actors, like the Sentiment Cards Manager or the mediator
 *  used for publish/subscribe events. 
 */
object Actors {

  /** The play framework provided application. */
  private var app: Application = _

  /** An optional custom actor system alternative to the one provided by play. */
  private var customSys: ActorSystem = _

  /** The mediator actor (with custom actor system) which handles publishings and subscriptions. */
  private lazy val customSysMediator= akka.contrib.pattern.DistributedPubSubExtension(customSys).mediator

  /** The manager actor (with custom actor system) which handles the creation and deletion of sentiment cards. */
  private lazy val customSysManager = customSys.actorOf(SentimentCardsManager.props(), "cards-manager")

  /** Gets the 'Actors' play plugin. */
  private def actors = app.plugin[Actors].getOrElse(sys.error("Actors plugin not registered"))

  /** Sets the play application. */
  def apply (_app: Application): Unit = if (app == null) app = _app

  /** Sets a custom system. */
  def apply (_sys: ActorSystem): Unit = if (customSys == null) customSys = _sys

  /** If no custom system was set returns the mediator from the 'Actors' plugin. */
  def mediator = 
    if (customSys == null) actors.mediator
    else customSysMediator

  /** If no custom system was set returns the manager from the 'Actors' plugin. */
  def sentimentCardsManager = 
    if (customSys == null) actors.sentimentCardsManager
    else customSysManager
}

class Actors(app: Application) extends Plugin {

  /** The mediator actor which handles publishings and subscriptions. */
  private lazy val mediator = akka.contrib.pattern.DistributedPubSubExtension(system).mediator

  /** The manager actor which handles the creation and deletion of sentiment cards. */
  private lazy val sentimentCardsManager = system.actorOf(SentimentCardsManager.props(), "cards-manager")

  /** The Akka system provided by the Play application. */
  def system = Akka.system(app)
}
