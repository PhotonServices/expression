/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import akka.actors.ActorSystem
import akka.contrib.pattern.DistributedPubSubExtension

package object scards {

  def eventbus (s: ActorSystem): ActorRef = DistributedPubSubExtension(s).mediator

  type Subscribe = akka.contrib.pattern.DistributedPubSubMediator.Subscribe

  type SubscribeAck = akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck

  type Unsubscribe = akka.contrib.pattern.DistributedPubSubMediator.Unsubscribe

  type UnsubscribeAck = akka.contrib.pattern.DistributedPubSubMediator.UnsubscribeAck

  type Publish = akka.contrib.pattern.DistributedPubSubMediator.Publish

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

}
