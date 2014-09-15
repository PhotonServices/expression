/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object Folksonomy {

  def props (id: String): Props = Props(new Folksonomy(id))
}

class Folksonomy (id: String) extends Actor with ActorLogging {

  def receive = {
    case _ => 
  }
}

