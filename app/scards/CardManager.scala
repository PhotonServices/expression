/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package scards

import collection.mutable.Map

import akka.actor._

/** Singleton actor which handles the creation and deletion
 *  of sentiment cards, which are abstracted by a [[actors.SentimentCard]]
 *  actor.
 *
 *  Also it subscribes to the 'client-subscription:card-new' event,
 *  which will notify him of every client which subscribes to the
 *  'card-new' event, for every notification the CardManager
 *  redirects the message to every SentimentCard actor.
 */
class CardManager (eventbus: ActorRef) extends Actor with ActorLogging {

  val cards = Map[String, ActorRef]()

  override def preStart() = eventbus ! Subscribe("client-subscription:card-new", self)

  def genId: String = java.util.UUID.randomUUID.toString filterNot(_ == '-')

  def createSentimentCard (id: String, name: String) =
    cards += (id -> context.actorOf(Props(classOf[SentimentCard], id, name, eventbus), id))

  def deleteSentimentCard (id: String) = context.child(id) match {
    case Some(child) =>
      child ! PoisonPill
      cards -= id
    case None => log.info("Tried to kill {} card but was already dead.", id)
  }

  def receive = {
    case CardNew(_, name) =>
      createSentimentCard(genId, name)

    case CardDelete(id) =>
      deleteSentimentCard(id)

    case ClientSubscription(event, socket) =>
      cards foreach { case (id, card) => card ! ClientSubscription(event, socket) }
  }
}
