/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package scards

import akka.actor._

/** Mantains the general statistics of the sentiment of
 *  a sentiment card.
 */
sealed abstract class Stats (card: String, eventbus: ActorRef) 
extends Stateful[StatsState]
with StateReporterActor {

  var state = StatsState(
    0f,
    Map(
    "excellent" -> 0f,
    "good" -> 0f,
    "neutral" -> 0f,
    "bad" -> 0f,
    "terrible" -> 0f),
    Map(
    "total" -> 0,
    "excellent" -> 0,
    "good" -> 0,
    "neutral" -> 0,
    "bad" -> 0,
    "terrible" -> 0))

  override def preStart() = {
    eventbus ! Subscribe(s"client-subscription:$card:sentiment-final", self)
    eventbus ! Subscribe(s"client-subscription:$card:sentiment-bars", self)
    amounts.foreach { case (sentiment, amount) =>
      eventbus ! Subscribe(s"client-subscription:$card:count-$sentiment", self)
    }
  }

  def recalculateSentimentAmount (sentiment: String) = {
    amounts(sentiment) = amounts(sentiment) + 1
    eventbus ! Publish(s"$card:count-$sentiment", AmountUpdate(card, sentiment, amounts(sentiment)))
  }

  def recalculateFinalSentiment = {
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
      eventbus ! Publish(s"$card:sentiment-final", SentimentUpdate(card, sentimentFinal))
  }

  def recalculateSentimentBars = {
    amounts - "total" map {
      case (sentiment, num) => sentimentBars(sentiment) = num * 100f / amounts("total")
    }
    eventbus ! Publish(s"$card:sentiment-bars", BarsUpdate(card, sentimentBars.toMap))
  }

  val FinalBarsRegExp = """.*:sentiment-(final|bars)""".r

  val SentimentRegExp = """.*count-(excellent|good|neutral|bad|terrible|total)""".r

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
