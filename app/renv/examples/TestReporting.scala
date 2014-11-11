/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv.examples

import renv._
import akka.actor.ActorRef

class ReportingTest (val eventbus: ActorRef) extends StatefulTest with StateReporterActor
