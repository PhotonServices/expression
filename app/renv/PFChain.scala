/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv

trait PFChain[A, B] { 
  type PF = PartialFunction[A, B]
  object EmptyPF extends PF {
    def isDefinedAt(x: A) = false
    def apply(x: A) = throw new UnsupportedOperationException("Empty behavior apply()")
  }
  var chain: PF = EmptyPF
  def firstLink: PF = EmptyPF
  def lastLink: PF = EmptyPF
  def linkToChain (l: PF) = chain = chain orElse l
  def closeChain: PF = firstLink orElse chain orElse lastLink
}
