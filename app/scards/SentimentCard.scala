/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package scards 

import akka.actor._

/** Supervised by [[actors.CardsManager]], this actor manages
 *  the processing of comments over a sentiment card. It uses
 *  static children to further process comment data and obtain
 *  semantic characteristics for the sentiment card.
 *
 *  The current children are: [[actors.Stats]], [[actors.Folksonomy]].
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
 *  through a [[actors.CardsManager.CardNew]] message to the
 *  subscribed socket.
 */
class SentimentCard (id: String, name: String, eventbus: ActorRef) extends Actor {

  /** [[Stats]] actor for this card. */
  val stats: ActorRef  = context.actorOf(Props(classOf[Stats], id, eventbus), s"$id:stats")

  /** [[Folksonomy]] actor for this card. */
  val folksonomy: ActorRef = context.actorOf(Props(classOf[Folksonomy], id, eventbus), s"$id:folksonomy")

  /** On creation publish it to the clients. */
  override def preStart () = eventbus ! Publish("card-new", CardNew(id, name))

  /** On deletion publish it to the clients. */
  override def postStop () = eventbus ! Publish("card-delete", CardDelete(id))

  /** Creates a new [[SentimentAPIRequester]] actor to delegate the processing of a comment. */
  def processComment (comment: Comment) = println("!!! process text not yet implemented !!!")

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
