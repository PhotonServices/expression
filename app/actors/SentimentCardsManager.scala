/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

/** Companion object with all the messages that involves 
 *  interaction with this actor.
 */
object SentimentCardsManager {

  /** Constructor for [[SentimentCardsManager]] actor props. 
   *
   * @return Props of SentimentCardsManager. 
   */
  def props: Props = Props(new SentimentCardsManager)
}

/** Singleton actor which handles the creation and deletion
 *  of sentiment cards, which are abstracted by a [[actors.SentimentCard]]
 *  actor.
 *
 *  Also it subscribes to the 'client-subscription:card-new' event,
 *  which will notify him of every client which subscribes to the
 *  'card-new' event, for every notification the SentimentCardsManager
 *  redirects the message to every SentimentCard actor.
 */
class SentimentCardsManager extends Actor with ActorLogging {

  import collection.mutable.Map

  val cards = Map[String, ActorRef]()

  override def preStart() = {
    Actors.mediator ! Subscribe("client-subscription:card-new", self)
    Argument("all meta") >>= Mongo.scards.getScards match {
      case FailedQuery(error) => throw error 
      case Result(scards) => scards foreach { card => createSentimentCard(card.id, card.name) }
    }
  }

  def genId: String = java.util.UUID.randomUUID.toString

  def dbCreateSentimentCard (id: String, name: String) = 
    Argument(Scard(id, name)) >>= Mongo.scards.createScard match {
      case _ => createSentimentCard(id, name)
    }

  def createSentimentCard (id: String, name: String) =
    cards += (id -> context.actorOf(SentimentCard.props(id, name), id))

  def dbDeleteSentimentCard (id: String) =
    Argument(Scard(id, "")) >>= Mongo.scards.deleteScard match {
      case _ => deleteSentimentCard(id)
    }

  def deleteSentimentCard (id: String) = context.child(id) match {
    case Some(child) => 
      child ! akka.actor.PoisonPill
      cards -= id
    case None => log.info("Tried to kill {} card but was already dead.", id)
  }

  def receive = {
    case CardNew(_, name) => 
      dbCreateSentimentCard(genId, name)

    case CardDelete(id) => 
      dbDeleteSentimentCard(id)

    case ClientSubscription(event, socket) =>
      cards foreach { case (id, card) => card ! ClientSubscription(event, socket) }

    case Wake => log.info("SentimentCardsManager awake.")
  }
}
