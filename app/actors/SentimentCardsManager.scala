/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCardsManager {
  def props (): Props = Props(new SentimentCardsManager)
}

class SentimentCardsManager extends Actor with ActorLogging {
  
  import play.api.libs.json._
  import WebSocketRouter.{ClientIn, ClientOut}
  import akka.contrib.pattern.DistributedPubSubExtension
  import akka.contrib.pattern.DistributedPubSubMediator.Publish
  import scala.collection.mutable.Map


  /** The mediator actor which handles publishings and subscriptions. */
  val mediator = DistributedPubSubExtension(context.system).mediator

  val cards = Map[String, ActorRef]()

  def receive = {
    case ClientIn(_, message, content) => message match {
      case "card-new" => mediator ! Publish("card-new", ClientOut("card-new", Json.obj("name" -> content)))
    }
    case _ => println("LLEGO!!!!!!!!")
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
