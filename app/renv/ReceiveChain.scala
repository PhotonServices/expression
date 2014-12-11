/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv

trait ReceiveChain extends PFChain[Any, Unit] {
  def lastly: PartialFunction[Any, Unit] = EmptyPF
  override def lastLink = lastly
}
