/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import akka.actor.{ActorSystem, ActorRef}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator

package object scards {

  protected[scards] def eventbus (s: ActorSystem): ActorRef = DistributedPubSubExtension(s).mediator

  protected[scards] val Publish = DistributedPubSubMediator.Publish

  protected[scards] val Subscribe = DistributedPubSubMediator.Subscribe

  protected[scards] val SubscribeAck = DistributedPubSubMediator.SubscribeAck

  protected[scards] val Unsubscribe = DistributedPubSubMediator.Unsubscribe

  protected[scards] val UnsubscribeAck = DistributedPubSubMediator.UnsubscribeAck

  protected[scards] case class Scard (id: String, name: String)

  protected[scards] case class CardNew (card: Scard)

  protected[scards] case class CardDelete (card: Scard)

  protected[scards] case object Add
  // OLD

  protected[scards] case class FolksonomyWord (sentiment: String, word: String)

  protected[scards] case class FolksonomyUpdate (card: String, sentiment: String, action: String, word: String)

  protected[scards] case class Comment (comment: String)

  protected[scards] case class CommentData (sentiment: String, folksonomies: List[String])

  protected[scards] case class Sentiment (sentiment: String)

  protected[scards] case class AmountUpdate (card: String, sentiment: String, amounts: Int)

  protected[scards] case class SentimentUpdate (card: String, value: Float)

  protected[scards] case class BarsUpdate (card: String, sentimentBars: Map[String, Float])

  protected[scards] case class ClientSubscription (event: String, socket: ActorRef)
}
