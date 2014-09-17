/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCard {
  
  case class Comment (comment: String)

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
    CommentAck,
    CommentData}

  import SentimentStats.{
    Sentiment}

  import Folksonomy.{
    FolksonomyWord}

  val stats = context.actorOf(SentimentStats.props(id), "stats")

  val folksonomy = context.actorOf(Folksonomy.props(id), "folksonomy")

  override def preStart () = Actors.mediator ! Publish("card-new", CardNew(id, name))

  override def postStop () = Actors.mediator ! Publish("card-delete", CardDelete(id))

  def genId: String = java.util.UUID.randomUUID.toString

  def processComment (comment: Comment) =
      context.actorOf(SentimentAPIRequester.props(self), genId).forward(comment)

  def processSentiment (sentiment: String) =
    stats ! Sentiment(sentiment)

  def processFolksonomy (sentiment: String, folklist: List[String]) =
    folklist foreach { word => folksonomy ! FolksonomyWord(sentiment, word) }

  def receive = {
    case msg: Comment => 
      sender ! CommentAck
      processComment(msg)

    case CommentData(sentiment, folklist) =>
      processSentiment(sentiment)
      processFolksonomy(sentiment, folklist)
  }

}
