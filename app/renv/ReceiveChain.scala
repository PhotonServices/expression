/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv

trait ReceiveChain extends PFChain[Any, Unit] {
  def lastly: PartialFunction[Any, Unit]
  override def lastLink = lastly
}
