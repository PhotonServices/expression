/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import akka.contrib.pattern.DistributedPubSubMediator

package object stateful {

  case class UpdateMe (topic: String, state: Any)

  protected[scards] val Publish = DistributedPubSubMediator.Publish

  protected[scards] val Subscribe = DistributedPubSubMediator.Subscribe

}
