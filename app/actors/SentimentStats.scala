/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentStats {

  def props: Props = Props(new SentimentStats)
}

class SentimentStats extends Actor with ActorLogging {

  def receive = {
    case _ => 
  }
}
