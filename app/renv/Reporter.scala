/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv

import scala.reflect.runtime.universe._
import akka.actor.{Actor, ActorRef}

trait Reporter {
  type ReportWriting = PartialFunction[Any, Report]
  def buildReport: ReportWriting
  def sendReport (report: Report): Unit
  def report (a: Any): Unit = sendReport(buildReport(a))
}

trait StateReporter extends Reporter {
  this: Stateful[_] =>
  onMutation( (oldState, newState) => report((oldState, newState)) )
  def stateClassification: String
  def buildReport: ReportWriting = {
    case (x: List[Any], y: List[Any]) => 
      val removes = x diff y
      val adds = y diff x
      var moves: List[ListMutation] = Nil
      if (!removes.isEmpty)
        moves = ListRemove(removes) :: moves
      if (!adds.isEmpty)
        moves = ListAdd(adds) :: moves
      Report(stateClassification, moves)
    case (oldState, newState) => Report(stateClassification, newState)
  }
}

trait EventbusReporter extends Reporter with EventbusActor {
  def sendReport (report: Report) = 
    publish(report.classification, report)
}

trait StateReporterActor 
extends EventbusReporter 
with StateReporter 
with ReceiveChain {
  this: Stateful[_] =>
  def stateClassification = s"$busid:state"
  linkToChain {
    case UpdateMe => sender ! Report(stateClassification, state)
  }
}
