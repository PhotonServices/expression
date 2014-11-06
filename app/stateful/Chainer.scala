/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package stateful

import akka.actor.Actor

trait Chain { 

  var chain: Actor.Receive = Actor.emptyBehavior

  def lastly: Actor.Receive = Actor.emptyBehavior

  def |<<<| (link: Actor.Receive) = chain = chain orElse link
}

trait Chainer extends Chain {
  this: Actor =>

  def receive = chain orElse lastly
}
