/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter

/** Companion of [[WebSocketRouter]] with the messages to interact with the actor and the constructor. */
object WebSocketRouter {

  /** Message to be enrouted by the [[WebSocketRouter]]. */
  case class ClientIn (path: String, message: String, content: String)

  /** Message to be delivered to the web socket client. */
  case class ClientOut (event: String, data: JsValue)

  /** [[ClientIn]] companion which holds the formaters needed to convert from json. */
  object ClientIn {
    implicit val messageFormater = Json.format[ClientIn]
    implicit val messageFrameFormater = FrameFormatter.jsonFrame[ClientIn]
  }

  /** [[ClientOut]] companion which holds the formaters needed to convert to json. 
   *
   * This message is used to publish events by other actors to be sent to the client.
   */
  object ClientOut {
    implicit val messageFormater = Json.format[ClientOut]
    implicit val messageFrameFormater = FrameFormatter.jsonFrame[ClientOut]
  }

  /** Constructor for [[WebSocketRouter]] actor props. 
   *
   * @param out actor which handles messages to the client. 
   * @return Props of WebSocketRouter. 
   */
  def props(out: ActorRef) = Props(new WebSocketRouter(out))
}

/** Actor which enroutes messages and events from and to the client.
 * 
 *  The actor enroutes messages from web socket clients and delivers
 *  events to which the client is subscribed. It also manages the
 *  subscriptions to specific events which the client can subscribe to,
 *  this way the client receives only the data it needs.
 *
 *  Each time the client subscribes to an event this actor also publishes
 *  an other event with this format 'client-subscription:<event-name>',
 *  publishing an [[actors.ClientSubscription]] message.
 *  this way other actors can be informed about clients subscribing to events,
 *  so that the actors can push their most recent state.
 *
 * @see [[https://www.playframework.com/documentation/2.3.x/ScalaWebSockets Play Framework WebSockets documentation]] to further understand the functionality of this actor.   
 */
class WebSocketRouter (out: ActorRef) extends Actor {

  import context.dispatcher
  import scala.concurrent.duration._
  import WebSocketRouter.{ClientIn, ClientOut}

  /** Regular expression. */
  val ChildCardPattern = "^(.*)/(.*)".r

  /** Stores the object that can cancell the echo intervals. */
  var echoCanceller: Cancellable = null

  /** Send an event to the client. */
  def emit (event: String, data: JsValue) = out ! ClientOut(event, data)

  /** Send an error event to the client. */
  def error (description: String) = emit("error", Json.toJson(description))

  /** Optional string to int. */
  def parseTime (int: String): Option[FiniteDuration] =
    try { Some(int.toInt milliseconds) }
    catch { case _: Exception => None }

  /** Starts an interval with a time string of seconds. */
  def scheduleEcho (time: String): Option[Cancellable] = parseTime(time) match {
    case Some(time) => Some(context.system.scheduler.schedule(
      0 milliseconds,
      time,
      out,
      ClientOut("echo", Json.toJson("tick"))))
    case None => None
  }

  /** Messages to start an echo. */
  def messageToEchoer (message: String, content: String) = message match {
    case "interval" => 
      if (echoCanceller == null) scheduleEcho(content) match {
        case Some(canceller) => echoCanceller = canceller
        case None => error("Not an integer")
      }
      else {
        echoCanceller.cancel
        echoCanceller = null
      }
    case _ => emit("echo", Json.toJson(s"$message $content"))
  }

  /** Messages to the events mediator. */
  def messageToMediator (message: String, content: String) = message match {
    case "subscribe" => 
      Actors.mediator ! Publish(s"client-subscription:$content", ClientSubscription(content, self))
      Actors.mediator ! Subscribe(content, self)
    case "unsubscribe" => Actors.mediator ! Unsubscribe(content, self)
    case _ => error(s"No such message $message.")
  }

  /** Messages to the sentiment cards manager. */
  def messageToManager (message: String, content: String) = message match {
    case "card-new" => Actors.sentimentCardsManager ! CardNew("", content)
    case "card-delete" => Actors.sentimentCardsManager ! CardDelete(content)
    case _ => error(s"No such message $message.")
  }

  /** Messages to sentiment cards. */
  def messageToCards (cards: ActorSelection, message: String, content: String) = message match {
    case "comment" => cards ! Comment(content)
    case _ => error(s"No such message $message.")
  }

  def receive = {
    /** Messages from the client to the actors system. */
    case ClientIn(path, message, content) => path match {
      case "echo" => 
        messageToEchoer(message, content) 

      case "events" => 
        messageToMediator(message, content)

      case "cards-manager" => 
        messageToManager(message, content)

      case ChildCardPattern(manager, card) => 
        messageToCards(context.actorSelection(s"/user/$manager/$card"), message, content)

      case _ => 
        error(s"No such path '$path'.")
    }

    /** Events from the actors system to the client. */
    case SubscribeAck(Subscribe(event, _, _)) => 
      emit("subscribe", Json.toJson(event)) 

    case UnsubscribeAck(Unsubscribe(event, _, _)) => 
      emit("unsubscribe", Json.toJson(event))

    case TestEvent(data) => 
      emit("test", Json.toJson(data))

    case CardNew(id, name) => 
      emit("card-new", Json.obj("id" -> id, "name" -> name))

    case CardDelete(name) => 
      emit("card-delete", Json.toJson(name))

    case AmountUpdate(card, sentiment, amount) => 
      emit(s"$card:count-$sentiment", Json.toJson(amount))

    case SentimentUpdate(card, value) => 
      emit(s"$card:sentiment-final", Json.toJson(value)) 

    case BarsUpdate(card, bars) => 
      emit(s"$card:sentiment-bars", Json.toJson(bars)) 

    case FolksonomyUpdate(card, sentiment, action, word) =>
      emit(s"$card:folksonomy-$sentiment:$action", Json.toJson(word))
  }
}
