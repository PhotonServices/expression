/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package stateful

trait Reporter[A] extends Stateful[A] with Chain {

  type Report = PartialFunction[(Any, A, A), (String, Any)]

  val eventbus: ActorRef

  def report: Report

  private def sendReport (r: (String, Any)) = eventbus ! Publish(r._1, r._2)

  onMutation { (a, b, c) => sendReport(report(a, b, c)) }
}
