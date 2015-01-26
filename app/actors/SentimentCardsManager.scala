/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

/** Companion object with all the messages that involves 
 *  interaction with this actor.
 */
object SentimentCardsManager {

  /** Message to create a new sentiment card. */
  case class CardNew (id: String, name: String)

  /** Message to delete an existing sentiment card. */
  case class CardDelete (id: String)

  case object Wake

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

  import akka.contrib.pattern.DistributedPubSubMediator.{
    Publish,
    Subscribe}

  import akka.actor.{
    PoisonPill}

  import SentimentCardsManager.{
    CardNew,
    CardDelete,
    Wake}

  import WebSocketRouter.{
    ClientSubscription}

  val cards = Map[String, ActorRef]()

  override def preStart() = {
    Actors.mediator ! Subscribe("client-subscription:card-new", self)
    Mongo.scards.getScards("all meta") match {
      case FailedQuery(error) => throw error 
      case Result(scards) => scards foreach { card => createSentimentCard(card.id, card.name) }
    }
  }

  def genId: String = java.util.UUID.randomUUID.toString

  def createSentimentCard (id: String, name: String) =
    cards += (id -> context.actorOf(SentimentCard.props(id, name), id))

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

    case Wake => log.info("SentimentCardsManager awake.")
  }
}
