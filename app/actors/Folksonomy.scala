/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object Folksonomy {

  def props: Props = Props(new Folksonomy)
}

class Folksonomy extends Actor with ActorLogging {

  def receive = {
    case _ => 
  }
}

