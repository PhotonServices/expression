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
  import SentimentCard.{
    Comment,
    CommentData}

  val s = new Sentiment("es")

  /** Changes a float value to it's correspondent string form. */
  def sentimentToString (sentiment: Float): String = sentiment match {
    case 2.0 => "excellent"
    case 1.0 => "good"
    case 0.0 => "neutral"
    case -1.0 => "bad"
    case -2.0 => "terrible"
  }

  /** Transforms a sentimen tag in english to spanish for the sentiment api. */
//  def enTOes (sentiment: String): String = sentiment match {
//    case "excellent" => "muy_positivos"
//    case "good" => "positivos"
//    case "neutral" => "neutros"
//    case "bad" => "negativos"
//    case "terrible" => "muy_negativos"
//  }

  def receive = {
    case Comment(comment) => 
      val buff = s.sentiment(comment)
      buff foreach { case (sentence, sentiment, tag, intensity) =>
        if (tag == "sentiment") {
          val wordcloud = s.wordCloud(sentence).toList
          val sentf = sentimentToString(sentiment.toFloat)
          receptor ! CommentData(sentf, wordcloud)
        }
      }
//      request(comment) map { response => 
//      val sentiment = sentimentToString((response.json \ "Overall Sentiment").as[Float])
//      val data = (response.json \ "folksonomies").as[Array[Map[String, Map[String, Int]]]]
//      val mapa = (data.flatMap(x=>x).toMap)
//      val folk = mapa(enTOes(sentiment)).map({ case (word, hits) => word }).toList 
//      receptor ! CommentData(sentiment, folk)
//    } recover { case e: Exception => throw e }
  }
}

