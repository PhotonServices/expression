package actors

import akka.actor._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter

case class ClientMessage (path: String, message: String, content: String)

object ClientMessage {
  implicit val messageFormater = Json.format[ClientMessage]
  implicit val messageFrameFormater = FrameFormatter.jsonFrame[ClientMessage]
}

object WebSocketRouterActor {
  def props(out: ActorRef) = Props(new WebSocketRouterActor(out))
}

class WebSocketRouterActor (out: ActorRef) extends Actor {

  def receive = {
    case message: ClientMessage => out ! Json.obj("got" -> "it")
  }

}
