/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCard {
  
  case class CardNew (id: String, name: String)

  case class CardDelete (id: String)

  case class Comment (body: String)

  case object CommentAck

  def props (id: String, name: String): Props = 
    Props(new SentimentCard(id: String, name: String))
}

class SentimentCard (id: String, name: String) extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish
  import SentimentCard.{
    CardNew,
    CardDelete,
    Comment,
    CommentAck
  }

  val stats = context.actorOf(SentimentStats.props, "stats")

  val folksonomy = context.actorOf(Folksonomy.props, "folksonomy")

  override def preStart () = Actors.mediator ! Publish("card-new", CardNew(id, name))

  override def postStop () = Actors.mediator ! Publish("card-delete", CardDelete(id))

  def receive = {
    case Comment(body) => sender ! CommentAck
  }

}
