/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object SentimentCard {
  def props (): Props = Props(new SentimentCard)
}

class SentimentCard extends Actor {

  def receive = {
    case _ => 
  }

}
