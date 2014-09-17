/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCard {
  
  case class Comment (body: String)

  case object CommentAck

  case class CommentData (sentiment: String, folksonomies: List[String])

  def props (id: String, name: String): Props = 
    Props(new SentimentCard(id: String, name: String))
}

class SentimentCard (id: String, name: String) extends Actor {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish

  import SentimentCardsManager.{
    CardNew,
    CardDelete}

  import SentimentCard.{
    Comment,
    CommentAck
  }

  val stats = context.actorOf(SentimentStats.props(id), "stats")

  val folksonomy = context.actorOf(Folksonomy.props(id), "folksonomy")

  override def preStart () = Actors.mediator ! Publish("card-new", CardNew(id, name))

  override def postStop () = Actors.mediator ! Publish("card-delete", CardDelete(id))

  def receive = {
    case Comment(body) => sender ! CommentAck
  }

}
