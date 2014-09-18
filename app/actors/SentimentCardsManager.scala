/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCardsManager {

  case class CardNew (id: String, name: String)

  case class CardDelete (id: String)

  /** Constructor for [[SentimentCardsManager]] actor props. 
   *
   * @return Props of SentimentCardsManager. 
   */
  def props: Props = Props(new SentimentCardsManager)
}

class SentimentCardsManager extends Actor with ActorLogging {

  import collection.mutable.Map

  import akka.contrib.pattern.DistributedPubSubMediator.{
    Publish,
    Subscribe}

  import akka.actor.{
    PoisonPill}

  import SentimentCardsManager.{
    CardNew,
    CardDelete}

  import WebSocketRouter.{
    ClientSubscription}

  val cards = Map[String, ActorRef]()

  override def preStart() = Actors.mediator ! Subscribe("client-subscription:card-new", self)

  private def genId: String = java.util.UUID.randomUUID.toString

  private def createSentimentCard (id: String, name: String) =
    cards += (id -> context.actorOf(SentimentCard.props(id, name), id))

  private def deleteSentimentCard (id: String) = context.child(id) match {
    case Some(child) => 
      child ! PoisonPill
      cards -= id
    case None => log.info("Tried to kill {} but was already dead.")
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
