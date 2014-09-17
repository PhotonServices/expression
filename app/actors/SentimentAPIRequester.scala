/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

import play.api.libs.json._

object SentimentAPIRequester {

  /** Constructor for [[SentimentAPIRequester]] actor props. */
  def props (receptor: ActorRef): Props = 
    Props(new SentimentAPIRequester(receptor: ActorRef))
}

class SentimentAPIRequester (receptor: ActorRef) extends Actor {

  import context.dispatcher
  import play.api.Play.current
  import play.api.libs.json._
  import play.api.libs.ws._
  import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
  import scala.concurrent.Future

  import SentimentCard.{
    Comment,
    CommentData}

  def serviceURL = current.configuration.getString("sentiment.service") match {
    case Some(url) => url
    case None => throw new Exception("Expected 'sentiment.service' configuration option in the play framework configuration file.")
  }

  def request (comment: String) =
    WS.url(serviceURL).post(Json.obj(
      "control" -> Json.obj(
        "classifier" -> "automatic",
        "no_classes" -> "default",
        "response" -> Json.obj(
          "folksonomies" -> true,
          "comments" -> "nothing"
        )),
      "data" -> Json.arr(Json.obj(
        "comment" -> comment,
        "id" -> 1
      ))
    ))

  def sentimentToString (sentiment: Float): String = sentiment match {
    case 2.0 => "excellent"
    case 1.0 => "good"
    case 0.0 => "neutral"
    case -1.0 => "bad"
    case -2.0 => "terrible"
  }

  def enTOes (sentiment: String): String = sentiment match {
    case "excellent" => "muy_positivos"
    case "good" => "positivos"
    case "neutral" => "neutral"
    case "bad" => "negativos"
    case "terrible" => "muy_negativos"
  }

  def receive = {
    case Comment(comment) => request(comment) map { response => 
      val sentiment = sentimentToString((response.json \ "Overall Sentiment").as[Float])
      val data = (response.json \ "folksonomies").as[Array[Map[String, Map[String, Int]]]]
      val mapa = (data.flatMap(x=>x).toMap)
      val folk = mapa(enTOes(sentiment)).map({ case (word, hits) => word }).toList 
      receptor ! CommentData(sentiment, folk)
    }
  }
}
