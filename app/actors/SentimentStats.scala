/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentStats {

  case class Sentiment (sentiment: String) 

  case class AmountUpdate (card: String, sentiment: String, amount: Int)

  case class SentimentUpdate (card: String, value: Float)

  case class BarsUpdate (card: String, bars: Map[String, Float])

  def props (id: String): Props = Props(new SentimentStats(id))
}

class SentimentStats (id: String) extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish
  import collection.mutable.Map
  import SentimentStats.{
    Sentiment,
    SentimentUpdate,
    AmountUpdate,
    BarsUpdate}

  private var global = 0f

  private val bars = Map(
    "excellent" -> 0f,
    "good" -> 0f,
    "neutral" -> 0f,
    "bad" -> 0f,
    "terrible" -> 0f)

  private var total = 0

  private val amount = Map(
    "excellent" -> 0,
    "good" -> 0,
    "neutral" -> 0,
    "bad" -> 0,
    "terrible" -> 0)

  private def recalculateTotalAmount (implicit sentiment: String) = {
    total = total + 1
    Actors.mediator ! Publish(s"$id:count-total", AmountUpdate(id, "total", total))
  }

  private def recalculateSentimentAmount (implicit sentiment: String) = {
    amount(sentiment) = amount(sentiment) + 1 
    Actors.mediator ! Publish(s"$id:count-$sentiment", AmountUpdate(id, sentiment, amount(sentiment)))
  }

  private def recalculateFinalSentiment (implicit sentiment: String) = {
    val lastSentiment = global
    global = amount.foldLeft(0f) { 
      case (sum, (sentiment, num)) => sentiment match {
        case "excellent" => sum + num * 2f
        case "good" => sum + num * 1f
        case "neutral" => sum
        case "bad" => sum + num * -1f
        case "terrible" => sum + num * -2f
      }
    } / total
    if (lastSentiment != global)
      Actors.mediator ! Publish(s"$id:sentiment-final", SentimentUpdate(id, global))
  }

  private def recalculateBars (implicit sentiment: String) = {
    amount.map { 
      case (sentiment, num) => bars(sentiment) = num * 100f / total
    }
    Actors.mediator ! Publish(s"$id:sentiment-bars", BarsUpdate(id, bars.toMap))
  }

  def receive = {
    case Sentiment(s) => 
      implicit val sentiment = s
      recalculateTotalAmount
      recalculateSentimentAmount
      recalculateFinalSentiment
      recalculateBars
  }
}
