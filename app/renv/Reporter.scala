/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv

import akka.actor.{Actor, ActorRef}

trait Reporter {
  type ReportWriting = PartialFunction[Any, Report]
  def buildReport: ReportWriting
  def sendReport (report: Report): Unit
  def report (a: Any): Unit = sendReport(buildReport(a))
}

trait StateReporter extends Reporter {
  this: Stateful[_] =>
  onMutation(report(_))
  def stateClassification: String
  def buildReport: ReportWriting = {
    case state => Report(stateClassification, state)
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
