/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

/** Companion object with all the messages that involves 
 *  interaction with [[actors.SentimentCard]] actors.
 */
object SentimentCard {

  /** Message to move a comment between actors. */
  case class Comment (comment: String)

  /** Message to move a processed comment data between actors. */
  case class CommentData (sentiment: String, folksonomies: List[String])

  def props (id: String, name: String): Props = 
    Props(new SentimentCard(id: String, name: String))
}

/** Supervised by [[actors.SentimentCardsManager]], this actor manages
 *  the processing of comments over a sentiment card. It uses
 *  static children to further process comment data and obtain 
 *  semantic characteristics for the sentiment card.
 *
 *  The current children are: [[actors.SentimentStats]], [[actors.Folksonomy]].
 *
 *  The complete work flow:
 *
 *  (1) This [[actors.SentimentCard]] receives an [[actors.SentimentCard.Comment]]
 *  message, which is redirected to a newly born [[actors.SentimentAPIRequester]]
 *  actor.
 *
 *  (2) The SentimentAPIRequester sends an http request to the
 *  sentiment restful api.
 *
 *  (3) The SentimentAPIRequester receives the processed comment,
 *  formats it and sends it back to this SentimentCard using a
 *  [[actors.SentimentCard.CommentData]] message.
 *
 *  (4) This SentimentCard destroys the api requester and redirects
 *  the data to it's children.
 *
 *  (5) The children reactively respond by publishing any changes on
 *  the semantic characteristics that they manage. Web clients can
 *  subscribe to this events through the [[actors.WebSocketRouter]],
 *  so that they receive updates on the semantic characteristics of
 *  the sentiment card.
 *
 *  This actor also receives redirections of [[actors.WebSocketRouter.ClientSubscription]]
 *  messages, which notify that a client through a web socket subscribed
 *  to the 'new-card' event, when notified this card pushes his information
 *  through a [[actors.SentimentCardsManager.CardNew]] message to the 
 *  subscribed socket.
 */
class SentimentCard (id: String, name: String) extends Actor {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish

  import akka.actor.{
    PoisonPill}

  import WebSocketRouter.{
    ClientSubscription}

  import SentimentCardsManager.{
    CardNew,
    CardDelete}

  import SentimentCard.{
    Comment,
    CommentData}

  import SentimentStats.{
    Sentiment}

  import Folksonomy.{
    FolksonomyWord}

  /** [[SentimentStats]] actor for this card. */
  val stats: ActorRef  = context.actorOf(SentimentStats.props(id), s"$id:stats")

  /** [[Folksonomy]] actor for this card. */
  val folksonomy: ActorRef = context.actorOf(Folksonomy.props(id), s"$id:folksonomy")

  /** On creation publish it to the clients. */
  override def preStart () = Actors.mediator ! Publish("card-new", CardNew(id, name))

  /** On deletion publish it to the clients. */
  override def postStop () = Actors.mediator ! Publish("card-delete", CardDelete(id))

  def genId: String = java.util.UUID.randomUUID.toString

  /** Creates a new [[SentimentAPIRequester]] actor to delegate the processing of a comment. */
  def processComment (comment: Comment) =
      context.actorOf(SentimentAPIRequester.props(self), genId).forward(comment)

  def processSentiment (sentiment: String) =
    stats ! Sentiment(sentiment)

  def processFolksonomy (sentiment: String, folklist: List[String]) =
    folklist foreach { word => folksonomy ! FolksonomyWord(sentiment, word) }

  def receive = {
    case msg: Comment => 
      processComment(msg)

    case CommentData(sentiment, folklist) =>
      processSentiment(sentiment)
      processFolksonomy(sentiment, folklist)
      sender ! PoisonPill

    case ClientSubscription(event, socket) =>
      socket ! CardNew(id, name)
  }

}
