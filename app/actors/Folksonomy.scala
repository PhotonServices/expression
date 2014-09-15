/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object Folksonomy {

  case class FolksonomyUpdate (card: String, sentiment: String, folksonomy: List[String])

  def props (id: String): Props = Props(new Folksonomy(id))
}

class Folksonomy (id: String) extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish
  import collection.mutable.Map
  import SentimentCard.Folksonomies
  import Folksonomy.FolksonomyUpdate

  val global = Map[String, Map[String, Int]](
    "excellent" -> Map(),
    "good" -> Map(),
    "neutral" -> Map(),
    "bad" -> Map(),
    "terrible" -> Map())

  def receive = {
    case Folksonomies(folksonomies) => 
  }
}

