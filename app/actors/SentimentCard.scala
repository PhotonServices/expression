/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCard {
  
  case class CardNew (id: String, name: String)

  case class CardDelete (id: String)

  case class Comment (body: String)

  def props (id: String, name: String): Props = 
    Props(new SentimentCard(id: String, name: String))
}

class SentimentCard (id: String, name: String) extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish
  import SentimentCard.{
    CardNew,
    CardDelete,
    Comment
  }

  override def preStart () = Actors.mediator ! Publish("card-new", CardNew(id, name))

  def receive = {
    case Comment(body) => log.info("New comment: {}", body)
  }

}
