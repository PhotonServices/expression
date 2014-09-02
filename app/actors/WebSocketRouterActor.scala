package actors

import akka.actor._

object WebSocketRouterActor {

  def props(out: ActorRef) = Props(new WebSocketRouterActor(out))

}

class WebSocketRouterActor (out: ActorRef) extends Actor {

  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
  }

  override def postStop () = {
    // Unsubscribe from all the events.
  }

}
