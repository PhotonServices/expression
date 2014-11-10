/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv

import akka.actor._

trait Reporter {
  this: Stateful[A] =>

  type Report = PartialFunction[A, (String, Any)]

  def report: Report

  def sendReport (r: (String, Any)): Unit

  onMutation { a => sendReport(report(a)) }

  |<<<| {
    case UpdateMe(receiver) => receiver ! state
  }
}

trait EventbusReporter extends Reporter {
  this: Actor =>
  val eventsID = self.path.toString
  val eventbus: ActorRef
  def sendReport (r: (String, Any)): Unit = eventbus ! Publish(r._1, r._2)
  def plugUpdates = eventbus ! Subscribe(s"$eventsID:updates", 
}

def 

trait SimpleReporter extends EventbusReporter {
  this: Actor =>

  def report: Report = {
    case x => (self.path.toString+":
