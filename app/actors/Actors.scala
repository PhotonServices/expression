/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor.ActorSystem

/** Gives access to global actors: 
 *
 *  The sentiment cards manager: used to control creation and deletion of sentiment cards, actor of [[actors.SentimentCardsManager]].
 *  The mediator: used for publish/subscribe events. 
 */
object Actors {

  /** The play framework provided application. */
  private var app: Application = _

  /** An optional custom actor system alternative to the one provided by play. */
  private var customSys: ActorSystem = _

  /** The mediator actor (with custom actor system) which handles publishings and subscriptions. */
  private lazy val customSysMediator= akka.contrib.pattern.DistributedPubSubExtension(customSys).mediator

  /** The manager actor (with custom actor system) which handles the creation and deletion of sentiment cards. */
  private lazy val customSysManager = customSys.actorOf(SentimentCardsManager.props, "cards-manager")

  /** An actor to create demos. */
  private lazy val customSysDemoer = customSys.actorOf(Demoer.props, "demoer")

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

  def demoer =
    if (customSys == null) actors.demoer
    else customSysDemoer
}

/** Play framework plugin 'Actor', used to globally access important 
 *  actors through this plugin's companion object.
 *
 *  The companion object must be initialized with a playframework app
 *  or with an actor system. In production this plugin is initialized
 *  at the [[Global]] object in the 'onStart' hook.
 *
 *  The companion object gives direct access to this global actors:
 *
 *  (1) The sentiment cards manager: used to control creation and 
 *  deletion of sentiment cards, actor of [[actors.SentimentCardsManager]].
 *
 *  (2) The mediator: used for publish/subscribe events.
 *
 *  The publish/subscribe system is done with the DistributedPubSubExtension 
 *  located in the akka contributions packages.
 *
 * @see [[http://doc.akka.io/api/akka/2.2.3/index.html#akka.contrib.pattern.DistributedPubSubMediator DistributedPubSubExtension api.]]
 * @see [[http://doc.akka.io/docs/akka/2.2.3/contrib/distributed-pub-sub.html DistributedPubSubExtension documentation]] to understand the usage of this akka extension.
 * @see [[https://www.playframework.com/documentation/2.3.x/ScalaPlugins ScalaPlugins documentation]] for more information about the programming of ScalaPlugins.
 */
class Actors(app: Application) extends Plugin {

  /** The mediator actor which handles publishings and subscriptions. */
  private lazy val mediator = akka.contrib.pattern.DistributedPubSubExtension(system).mediator

  /** The manager actor which handles the creation and deletion of sentiment cards. */
  private lazy val sentimentCardsManager = system.actorOf(SentimentCardsManager.props, "cards-manager")

  /** An actor to create demos. */
  private lazy val demoer = system.actorOf(Demoer.props, "demoer")

  /** The Akka system provided by the Play application. */
  def system = Akka.system(app)
}
