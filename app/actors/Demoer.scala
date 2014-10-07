/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object Demoer {

  case class Commands (cmds: Array[String])

  case class ScheduleNewCard (name: String)

  case class ScheduleComment (card: String, cmt: String)

  def props: Props = Props(new Demoer)
}

/** An actor which schedules new sentiment cards and comments 
 *  to those sentiment cards, designed to create demos.
 * 
 *  This actor is accessible through [[actors.Actors.demoer]].
 *
 *  An example script using curl to send a schedule to this actor
 *  when the expression service is active in localhost:9000.
 * 
 *  @example {{{
 *  curl -X POST -H "Content-Type: application/json" -d '{
 *    "commands": [
 *      "1 new >>> Test Card",
 *      "2 cmt:Test-Card >>> Qué buena está la comida.",
 *      "3 cmt:Test-Card >>> El servicio me gustó.",
 *      "4 cmt:Test-Card >>> Los baños están asquerosos :(.",
 *      "5 cmt:Test-Card >>> Las meseras están bien bonitas hehe.",
 *      "6 cmt:Test-Card >>> Voy a volver varias veces con mucho gusto."
 *    ]
 *  }' http://localhost:9000/demoer
 *  }}}
 */
class Demoer extends Actor {

  import scala.collection.mutable.Map
  import scala.concurrent.duration._
  import context.dispatcher

  import Demoer.{
    Commands}

  import akka.contrib.pattern.DistributedPubSubMediator.{
    Subscribe}

  import Demoer.{
    ScheduleNewCard,
    ScheduleComment}

  import SentimentCard.{
    Comment}

  val scheduler = context.system.scheduler

  val cards = Map[String, ActorRef]()

  val NewReg = """([1-9]+) new >>> (.*)""".r

  val CmtReg = """([1-9]+) cmt:(.*) >>> (.*)""".r

  def receive = {
    case Commands(cmds) => cmds foreach { cmd => cmd match {
      case NewReg(seg, card) => 
        scheduler.scheduleOnce(seg.toInt seconds, self, ScheduleNewCard(card))
      case CmtReg(seg, card, cmt) => 
        scheduler.scheduleOnce(seg.toInt seconds, self, ScheduleComment(card, cmt))
    }}

    case ScheduleNewCard(name) => 
      val id = name.replace(' ', '-')
      cards += (id -> context.actorOf(SentimentCard.props(id, name), id))

    case ScheduleComment(card, cmt) =>
      cards(card) ! Comment(cmt)
  }
}
