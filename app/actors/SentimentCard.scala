/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

/** Companion object with all the messages that involves 
 *  interaction with [[actors.SentimentCard]] actors.
 */
object SentimentCard {

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
 *  (1) This [[actors.SentimentCard]] receives an [[actors.Comment]]
 *  message, which is redirected to a newly born [[actors.SentimentAPIRequester]]
 *  actor.
 *
 *  (2) The SentimentAPIRequester sends an http request to the
 *  sentiment restful api.
 *
 *  (3) The SentimentAPIRequester receives the processed comment,
 *  formats it and sends it back to this SentimentCard using a
 *  [[actors.CommentData]] message.
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
 *  This actor also receives redirections of [[actors.ClientSubscription]]
 *  messages, which notify that a client through a web socket subscribed
 *  to the 'new-card' event, when notified this card pushes his information
 *  through a [[actors.CardNew]] message to the 
 *  subscribed socket.
 */
class SentimentCard (id: String, name: String) extends Actor {

  val identity: Scard = Argument(Scard(id, name)) >>= Mongo.scards.getScard match {
    case Result(scard) => scard
    case _ => Scard(id, name)
  }

  /** [[SentimentStats]] actor for this card. */
  val stats: ActorRef  = context.actorOf(Props(new SentimentStats(id, identity)), s"$id:stats")

  /** [[Folksonomy]] actor for this card. */
  val folksonomy: ActorRef = context.actorOf(Props(new Folksonomy(id, identity)), s"$id:folksonomy")

  /** On creation publish it to the clients. */
  override def preStart () = Actors.mediator ! Publish("card-new", CardNew(id, name))

  /** On deletion publish it to the clients. */
  override def postStop () = Actors.mediator ! Publish("card-delete", CardDelete(id))

  def genId: String = java.util.UUID.randomUUID.toString

  /** Creates a new [[SentimentAPIRequester]] actor to delegate the processing of a comment. */
  def processComment (comment: Comment) =
    context.actorOf(Props(new Tinga(self, identity)), genId).forward(comment)
    //context.actorOf(SentimentAPIRequester.props(self), genId).forward(comment)

  def processSentiment (sentiment: String) =
    stats ! SentimentMsg(sentiment)

  def processFolksonomy (sentiment: String, folklist: List[String]) =
    folklist foreach { word => folksonomy ! FolksonomyWord(sentiment, word) }

  def receive = {
    case msg: Comment => 
      processComment(msg)

    case CommentData(sentiment, folklist) =>
      processSentiment(sentiment)
      processFolksonomy(sentiment, folklist)
      // Kill here if using the Python API. Comment otherwise.
      //sender ! PoisonPill

    case EndOfCommentData =>
      sender ! akka.actor.PoisonPill

    case ClientSubscription(event, socket) =>
      socket ! CardNew(id, name)
  }

}
