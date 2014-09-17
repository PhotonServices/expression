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

  import akka.actor.PoisonPill
  import SentimentCardsManager.{
    CardNew,
    CardDelete
  }

  def genId: String = java.util.UUID.randomUUID.toString

  def createSentimentCard (id: String, name: String) =
    context.actorOf(SentimentCard.props(id, name), id)

  def receive = {
    case CardNew(_, name) => 
      createSentimentCard(genId, name)

    case CardDelete(id) => context.child(id) match {
      case Some(child) => child ! PoisonPill
      case None => log.info("Tried to kill {} but was already dead.")
    }
  }
}
