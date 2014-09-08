/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter

/** Companion of [[WebSocketRouter]] with the messages to interact with the actor and the constructor. */
object WebSocketRouter {

  /** Message to be enrouted by the [[WebSocketRouter]]. */
  case class ClientIn (path: String, message: String, content: String)

  /** Message to be delivered to the web socket client. */
  case class ClientOut (event: String, data: JsValue)

  /** [[ClientIn]] companion which holds the formaters needed to convert from json. */
  object ClientIn {
    implicit val messageFormater = Json.format[ClientIn]
    implicit val messageFrameFormater = FrameFormatter.jsonFrame[ClientIn]
  }

  /** [[ClientOut]] companion which holds the formaters needed to convert to json. 
   *
   * This message is used to publish events by other actors to be sent to the client.
   */
  object ClientOut {
    implicit val messageFormater = Json.format[ClientOut]
    implicit val messageFrameFormater = FrameFormatter.jsonFrame[ClientOut]
  }

  /** Constructor for [[WebSocketRouter]] ActorRefs. 
   *
   * @param out actor which handles messages to the client. 
   * @return an ActorRef of WebSocketRouter. 
   */
  def props(out: ActorRef) = Props(new WebSocketRouter(out))
}

/** Actor which enroutes messages and events from and to the client.
 * 
 * The actor enroutes messages from web socket clients and delivers
 * events to which the client is subscribed. It also manages the
 * subscriptions to specific events which the client can subscribe to,
 * this way the client receives only the data it needs.
 *
 * @constructor use the companion object method 'props'.
 */
class WebSocketRouter (out: ActorRef) extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.{
    Subscribe,
    SubscribeAck,
    Unsubscribe,
    UnsubscribeAck}
  import WebSocketRouter.{
    ClientIn, 
    ClientOut}
  import SentimentCard.{
    CardNew,
    CardDelete,
    Comment}

  /** Json formatter. */
  implicit private val cardNewFormatter = Json.format[CardNew]

  /** Json formatter. */
  implicit private val cardDeleteFormatter = Json.format[CardDelete]

  /** Regular expression. */
  private val ChildCardPattern = "^/(.*)/(.*)".r

  /** Messages to the events mediator. */
  private def messageToMediator (message: String, content: String) = message match {
    case "subscribe" => Actors.mediator ! Subscribe(content, self)
    case "unsubscribe" => Actors.mediator ! Unsubscribe(content, self)
  }

  /** Messages to the sentiment cards manager. */
  private def messageToManager (message: String, content: String) = message match {
    case "card-new" => Actors.sentimentCardsManager ! CardNew("", content)
    case "card-delete" => Actors.sentimentCardsManager ! CardDelete(content)
  }

  /** Messages to sentiment cards. */
  private def messageToCard (card: ActorSelection, message: String, content: String) = message match {
    case "comment" => card ! Comment(content)
  }

  /** Send an event to the client. */
  private def emit (event: String, data: JsValue) = out ! ClientOut(event, data)

  def receive = {
    /** Messages from the client to the actors system. */
    case ClientIn(path, message, content) => path match {
      case "/echo" => emit("echo", Json.toJson(content))
      case "/events" => messageToMediator(message, content)
      case "/cards-manager" => messageToManager(message, content)
      case ChildCardPattern(manager, card) => 
        messageToCard(context.actorSelection(s"akka://application/$manager/$card"), message, content)
    }

    /** Events from the actors system to the client. */
    case SubscribeAck(Subscribe(event, _, _)) => emit("subscribe", Json.toJson(event)) 
    case UnsubscribeAck(Unsubscribe(event, _, _)) => emit("unsubscribe", Json.toJson(event))
    case CardNew(id, name) => emit("card-new", Json.obj("id" -> id, "name" -> name))
    case CardDelete(name) => emit("card-delete", Json.toJson(name))
  }

}
