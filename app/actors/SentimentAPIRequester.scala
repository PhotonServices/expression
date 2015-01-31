/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

/** Companion object with a Props builder for this actor. */
object SentimentAPIRequester {

  /** Constructor for [[SentimentAPIRequester]] actor props. */
  def props (receptor: ActorRef): Props = 
    Props(new SentimentAPIRequester(receptor: ActorRef))
}

/** Supervised by [[actors.SentimentCard]], used to manage an http
 *  request to the sentiment restful api.
 *
 *  When an [[actors.Comment]] message is received it 
 *  inits an http request using play's [[https://www.playframework.com/documentation/2.3.x/ScalaWS WS]]
 *  library.
 *
 *  When the service responds, it formats the response and sends an
 *  [[actors.CommentData]] message back to it's sentiment
 *  card supervisor, which will kill him after that.
 */
class SentimentAPIRequester (receptor: ActorRef) extends Actor {

  import context.dispatcher
  import play.api.Play.current
  import play.api.libs.json._
  import play.api.libs.ws._
  import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
  import scala.concurrent.Future

  /** The url where the sentiment api resides, can be changed in the app configuration,
   * at /conf/application.conf with the attribute 'sentiment.service'.
   */
  def serviceURL = current.configuration.getString("sentiment.service") match {
    case Some(url) => url
    case None => throw new Exception("Expected 'sentiment.service' configuration option in the play framework configuration file.")
  }

  /** Sends a request to process a comment to the sentiment api. */
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

  /** Changes a float value to it's correspondent string form. */
  def sentimentToString (sentiment: Float): String = sentiment match {
    case 2.0 => "excellent"
    case 1.0 => "good"
    case 0.0 => "neutral"
    case -1.0 => "bad"
    case -2.0 => "terrible"
  }

  /** Transforms a sentimen tag in english to spanish for the sentiment api. */
  def enTOes (sentiment: String): String = sentiment match {
    case "excellent" => "muy_positivos"
    case "good" => "positivos"
    case "neutral" => "neutros"
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
    } recover { case e: Exception => throw e }
  }
}
