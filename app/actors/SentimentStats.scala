/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentStats {

  case class Sentiment (sentiment: String) 

  case class AmountUpdate (card: String, sentiment: String, amounts: Int)

  case class SentimentUpdate (card: String, value: Float)

  case class BarsUpdate (card: String, sentimentBars: Map[String, Float])

  def props (card: String): Props = Props(new SentimentStats(card))
}

class SentimentStats (card: String) extends Actor with ActorLogging {

  import collection.mutable.Map

  import akka.contrib.pattern.DistributedPubSubMediator.{
    Subscribe,
    Publish}

  import WebSocketRouter.{
    ClientSubscription}

  import SentimentStats.{
    Sentiment,
    SentimentUpdate,
    AmountUpdate,
    BarsUpdate}

  private var sentimentFinal = 0f

  private val sentimentBars = Map(
    "excellent" -> 0f,
    "good" -> 0f,
    "neutral" -> 0f,
    "bad" -> 0f,
    "terrible" -> 0f)

  private val amounts = Map(
    "total" -> 0,
    "excellent" -> 0,
    "good" -> 0,
    "neutral" -> 0,
    "bad" -> 0,
    "terrible" -> 0)

  override def preStart() = {
    Actors.mediator ! Subscribe(s"client-subscription:$card:sentiment-final", self)
    Actors.mediator ! Subscribe(s"client-subscription:$card:sentiment-sentimentBars", self)
    amounts.foreach { case (sentiment, amount) =>
      Actors.mediator ! Subscribe(s"client-subscription:$card:count-$sentiment", self)
    }
  }

  private def recalculateSentimentAmount (sentiment: String) = {
    amounts(sentiment) = amounts(sentiment) + 1 
    Actors.mediator ! Publish(s"$card:count-$sentiment", AmountUpdate(card, sentiment, amounts(sentiment)))
  }

  private def recalculateFinalSentiment = {
    val lastSentiment = sentimentFinal
    sentimentFinal = amounts.foldLeft(0f) { 
      case (sum, (sentiment, num)) => sentiment match {
        case "total" => sum
        case "excellent" => sum + num * 2f
        case "good" => sum + num * 1f
        case "neutral" => sum
        case "bad" => sum + num * -1f
        case "terrible" => sum + num * -2f
      }
    } / amounts("total") 
    if (lastSentiment != sentimentFinal)
      Actors.mediator ! Publish(s"$card:sentiment-final", SentimentUpdate(card, sentimentFinal))
  }

  private def recalculateSentimentBars = {
    amounts - "total" map {
      case (sentiment, num) => sentimentBars(sentiment) = num * 100f / amounts("total")
    }
    Actors.mediator ! Publish(s"$card:sentiment-bars", BarsUpdate(card, sentimentBars.toMap))
  }

  private val FinalBarsRegExp = """.*:sentiment-(final|stats)""".r

  private val SentimentRegExp = """.*count-(excellent|good|neutral|bad|terrible|total)""".r

  def receive = {
    case Sentiment(sentiment) => 
      recalculateSentimentAmount("total")
      recalculateSentimentAmount(sentiment)
      recalculateSentimentBars
      recalculateFinalSentiment

    case ClientSubscription(event, socket) => event match {
      case FinalBarsRegExp(update) =>
        if (update == "final")
          socket ! SentimentUpdate(card, sentimentFinal)
        else
        socket ! BarsUpdate(card, sentimentBars.toMap)
      case SentimentRegExp(sentiment)  =>
        socket ! AmountUpdate(card, sentiment, amounts(sentiment))
    }
  }
}
