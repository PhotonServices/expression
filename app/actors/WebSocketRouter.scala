/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._
import akka.actor.ActorLogging

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter

/** Companion of [[WebSocketRouter]] with the messages to interact with the actor and the constructor. */
object WebSocketRouter {

  /** Message to be enrouted by the [[WebSocketRouter]]. */
  case class ClientIn (path: String, message: String, content: String)

  /** [[ClientIn]] companion which holds the formaters needed to convert from json. */
  object ClientIn {
    implicit val messageFormater = Json.format[ClientIn]
    implicit val messageFrameFormater = FrameFormatter.jsonFrame[ClientIn]
  }

  /** Message to be delivered to the web socket client. */
  case class ClientOut (event: String, data: JsValue)

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

  import WebSocketRouter.{ClientIn, ClientOut}
  import akka.contrib.pattern.DistributedPubSubExtension
  import akka.contrib.pattern.DistributedPubSubMediator.{
    Send,
    Subscribe,
    SubscribeAck,
    Unsubscribe,
    UnsubscribeAck}

  /** The mediator actor which handles publishings and subscriptions. */
  val mediator = DistributedPubSubExtension(context.system).mediator

  def receive = {
    case ClientIn(path, message, content) => path match {
      case "/echo" => out ! ClientOut("echo", Json.obj(message -> content))
      case "/event-bus" => message match {
        case "subscribe" => mediator ! Subscribe(content, self)
        case "unsubscribe" => mediator ! Unsubscribe(content, self)
      }
      case _ => mediator ! Send(path, message, false)
    }

    case SubscribeAck(Subscribe(event, _, `self`)) => log.info(event)

    case message: ClientOut => out ! message
  }

}
