/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentAPIRequester {

  def props (comment: String): Props = 
    Props(new SentimentAPIRequester(comment: String))
}

class SentimentAPIRequester (comment: String) extends Actor with ActorLogging {

  def receive = {
    case _ => 
  }
}
