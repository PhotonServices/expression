/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

/** Companion object with a Props builder for this actor. */
object Tinga {

  /** Constructor for [[SentimentAPIRequester]] actor props. */
  def props (receptor: ActorRef): Props = 
    Props(new Tinga(receptor: ActorRef))
}

/** Supervised by [[actors.SentimentCard]], used compute sentiment analysis
 */
class Tinga (receptor: ActorRef) extends Actor {

  import tinga.sentiment._

  val s = new Sentiment("es")

  /** Changes a float value to it's correspondent string form. */
  def sentimentToString (sentiment: Double): String = sentiment match {
    case 2.0 => "excellent"
    case 1.0 => "good"
    case 0.0 => "neutral"
    case -1.0 => "bad"
    case -2.0 => "terrible"
  }

  def receive = {
    case Comment(comment) => 
      val buff = s.sentiment(comment)
      buff foreach { case (sentence, sentiment, tag, intensity) =>
        if (tag == "sentiment") {
          val wordcloud = s.wordCloud(sentence).toList
          val sentf = sentimentToString(sentiment)
          receptor ! CommentData(sentf, wordcloud)
        }
      }
      receptor ! EndOfCommentData
  }
}

