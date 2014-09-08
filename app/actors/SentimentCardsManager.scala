/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCardsManager {

  /** Constructor for [[SentimentCardsManager]] ActorRefs. 
   *
   * @return an ActorRef of SentimentCardsManager. 
   */
  def props (): Props = Props(new SentimentCardsManager)
}

class SentimentCardsManager extends Actor with ActorLogging {

  import SentimentCard.{
    CardNew,
    CardDelete
  }

  def genId: String = "id-temporal"

  def createSentimentCard (id: String, name: String) =
    context.actorOf(SentimentCard.props(id, name), id)

  def receive = {
    case CardNew(_, name) => createSentimentCard(genId, name)
  }
}
