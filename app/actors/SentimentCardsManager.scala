/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCardsManager {
  def props (): Props = Props(new SentimentCardsManager)
}

class SentimentCardsManager extends Actor with ActorLogging {

  import scala.collection.mutable.Map
  import play.api.libs.json._
  import WebSocketRouter.{
    ClientIn, 
    ClientOut}
  import akka.contrib.pattern.DistributedPubSubMediator.Publish

  val cards = Map[String, ActorRef]()

  def receive = {
    case ClientIn(_, message, content) => message match {
      case "card-new" => Actors.mediator ! Publish("card-new", ClientOut("card-new", Json.obj("name" -> content)))
    }
  }

}

object SentimentCard {
  def props (): Props = Props(new SentimentCard)
}

class SentimentCard extends Actor {

  def receive = {
    case _ => 
  }

}
