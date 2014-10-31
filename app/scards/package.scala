/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import akka.actor.{ActorSystem, ActorRef}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator

package object scards {

  def eventbus (s: ActorSystem): ActorRef = DistributedPubSubExtension(s).mediator

  val Publish = DistributedPubSubMediator.Publish

  val Subscribe = DistributedPubSubMediator.Subscribe

  val SubscribeAck = DistributedPubSubMediator.SubscribeAck

  val Unsubscribe = DistributedPubSubMediator.Unsubscribe

  val UnsubscribeAck = DistributedPubSubMediator.UnsubscribeAck

  case class CardNew (id: String, name: String)

  case class CardDelete (id: String)

  case class FolksonomyWord (sentiment: String, word: String)

  case class FolksonomyUpdate (card: String, sentiment: String, action: String, word: String)

  case class Comment (comment: String)

  case class CommentData (sentiment: String, folksonomies: List[String])

  case class Sentiment (sentiment: String)

  case class AmountUpdate (card: String, sentiment: String, amounts: Int)

  case class SentimentUpdate (card: String, value: Float)

  case class BarsUpdate (card: String, sentimentBars: Map[String, Float])

  case class ClientSubscription (event: String, socket: ActorRef)
}
