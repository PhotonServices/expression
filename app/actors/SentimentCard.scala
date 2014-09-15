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

  case class CommentData (sentiment: Float, folksonomies: Map[String, Array[String]])

  class SentimentFormatError (format: Float) extends Exception("Wrong sentiment format: "+format)

  case class Sentiment (sentiment: Float) {
    override def toString = sentiment match {
      case 2f => "excellent"
      case 1f => "good"
      case 0f => "neutral"
      case -1f => "bad"
      case -2f => "terrible"
      case _ => throw new SentimentFormatError(sentiment)
    }
  }

  case class Folksonomies (folksonomies: Map[String, Array[String]])

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

  val stats = context.actorOf(SentimentStats.props(id), "stats")

  val folksonomy = context.actorOf(Folksonomy.props(id), "folksonomy")

  override def preStart () = Actors.mediator ! Publish("card-new", CardNew(id, name))

  override def postStop () = Actors.mediator ! Publish("card-delete", CardDelete(id))

  def receive = {
    case Comment(body) => sender ! CommentAck
  }

}
