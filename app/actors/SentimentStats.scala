/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

/** Companion object with all the messages that involves 
 *  interaction with this actor.
 */
object SentimentStats {

  def props (card: String): Props = Props(new SentimentStats(card))
}

/** Mantains the general statistics of the sentiment of
 *  a sentiment card.
 */
class SentimentStats (card: String, initData: Scard = Scard("", "")) extends Actor {

  var sentimentFinal = initData.sentimentFinal

  /**
   * Map(
   * "excellent" -> 0d,
   * "good" -> 0d,
   * "neutral" -> 0d,
   * "bad" -> 0d,
   * "terrible" -> 0d)
   */
  val sentimentBars = initData.sentimentBars

  /**
   *  Map(
   *  "total" -> 0,
   *  "excellent" -> 0,
   *  "good" -> 0,
   *  "neutral" -> 0,
   *  "bad" -> 0,
   *  "terrible" -> 0)
   */
  val amounts = initData.amounts

  override def preStart() = {
    Actors.mediator ! Subscribe(s"client-subscription:$card:sentiment-final", self)
    Actors.mediator ! Subscribe(s"client-subscription:$card:sentiment-bars", self)
    amounts.foreach { case (sentiment, amount) =>
      Actors.mediator ! Subscribe(s"client-subscription:$card:count-$sentiment", self)
    }
  }

  def recalculateSentimentAmount (sentiment: String) = {
    amounts(sentiment) = amounts(sentiment) + 1 
    Argument((Scard(card, ""), amounts)) >>= Mongo.scards.updateAmounts
    Actors.mediator ! Publish(s"$card:count-$sentiment", AmountUpdate(card, sentiment, amounts(sentiment)))
  }

  def recalculateFinalSentiment = {
    val lastSentiment = sentimentFinal
    sentimentFinal = amounts.foldLeft(0d) { 
      case (sum, (sentiment, num)) => sentiment match {
        case "total" => sum
        case "excellent" => sum + num * 2d
        case "good" => sum + num * 1d
        case "neutral" => sum
        case "bad" => sum + num * -1d
        case "terrible" => sum + num * -2d
      }
    } / amounts("total")
    if (lastSentiment != sentimentFinal) {
      Argument((Scard(card, ""), sentimentFinal)) >>= Mongo.scards.updateSentimentFinal
      Actors.mediator ! Publish(s"$card:sentiment-final", SentimentUpdate(card, sentimentFinal))
    }
  }

  def recalculateSentimentBars = {
    amounts - "total" map {
      case (sentiment, num) => sentimentBars(sentiment) = num * 100d / amounts("total")
    }
    Argument((Scard(card, ""), sentimentBars)) >>= Mongo.scards.updateSentimentBars
    Actors.mediator ! Publish(s"$card:sentiment-bars", BarsUpdate(card, sentimentBars.toMap))
  }

  val FinalBarsRegExp = """.*:sentiment-(final|bars)""".r

  val SentimentRegExp = """.*count-(excellent|good|neutral|bad|terrible|total)""".r

  def receive = {
    case SentimentMsg(sentiment) => 
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
