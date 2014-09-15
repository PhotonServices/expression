/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentStats {

  case class AmountUpdate (card: String, sentiment: String, amount: Int)

  case class SentimentUpdate (card: String, value: Float)

  case class BarsUpdate (card: String, bars: Map[String, Float])

  def props (id: String): Props = Props(new SentimentStats(id))
}

class SentimentStats (id: String) extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish
  import collection.mutable.Map
  import SentimentCard.Sentiment
  import SentimentStats.{
    SentimentUpdate,
    AmountUpdate,
    BarsUpdate}

  var finalSentiment = 0f

  val bars = Map(
    "excellent" -> 0f,
    "good" -> 0f,
    "neutral" -> 0f,
    "bad" -> 0f,
    "terrible" -> 0f)

  var total = 0

  val amount = Map (
    "excellent" -> 0,
    "good" -> 0,
    "neutral" -> 0,
    "bad" -> 0,
    "terrible" -> 0)

  def receive = {
    case Sentiment(sentiment) => 
      // Recalculate total amount..
      total = total + 1
      Actors.mediator ! Publish(s"$id:count-total", AmountUpdate(id, "total", total))
      // Recalculate sentiment amount.
      amount(sentiment) = amount(sentiment) + 1 
      Actors.mediator ! Publish(s"$id:count-$sentiment", AmountUpdate(id, sentiment, amount(sentiment)))
      // Recalculate final sentiment.
      val lastSentiment = finalSentiment
      finalSentiment = amount.foldLeft(0f) { 
        case (sum, (sentiment, num)) => sentiment match {
          case "excellent" => sum + num * 2f
          case "good" => sum + num * 1f
          case "neutral" => sum
          case "bad" => sum + num * -1f
          case "terrible" => sum + num * -2f
        }
      } / total
      if (lastSentiment != finalSentiment)
        Actors.mediator ! Publish(s"$id:sentiment-final", SentimentUpdate(id, finalSentiment))
      // Recalculate bars.
      amount.map { 
        case (sentiment, num) => bars(sentiment) = num * 100f / total
      }
      Actors.mediator ! Publish(s"$id:sentiment-bars", BarsUpdate(id, bars.toMap))
  }
}
