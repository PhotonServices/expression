/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv

import akka.actor.{Actor, ActorRef}
import akka.contrib.pattern.DistributedPubSubMediator.{
  Publish,
  Subscribe
}

trait EventbusActor extends Actor {
  type Eventbus = ActorRef
  val eventbus: Eventbus
  val busid = self.path.toString
  def publish (event: String, a: Any) = eventbus ! Publish(event, a)
  def subscribe (event: String) = eventbus ! Subscribe(event, self)
}
