package actors

import akka.actor._
import akka.actor.ActorLogging

import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Send
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck
import akka.contrib.pattern.DistributedPubSubMediator.Unsubscribe
import akka.contrib.pattern.DistributedPubSubMediator.UnsubscribeAck

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter

case class ClientIn (path: String, message: String, content: String)
case class ClientOut (event: String, data: JsValue)
object ClientIn {
  implicit val messageFormater = Json.format[ClientIn]
  implicit val messageFrameFormater = FrameFormatter.jsonFrame[ClientIn]
}
object ClientOut {
  implicit val messageFormater = Json.format[ClientOut]
  implicit val messageFrameFormater = FrameFormatter.jsonFrame[ClientOut]
}

object WebSocketRouter {
  def props(out: ActorRef) = Props(new WebSocketRouter(out))
}

class WebSocketRouter (out: ActorRef) extends Actor with ActorLogging {

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
