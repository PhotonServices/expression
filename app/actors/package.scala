/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import akka.actor.{ActorSystem, ActorRef}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator

package object actors {

  type Sentiment = String
  type Word = String
  type Hits = Int

  /** Message to move a comment between actors. */
  case class Comment (comment: String)

  /** Message to move a processed comment data between actors. */
  case class CommentData (sentiment: String, folksonomies: List[String])

  /** Message to create a new sentiment card. */
  case class CardNew (id: String, name: String)

  /** Message to delete an existing sentiment card. */
  case class CardDelete (id: String)

  /** Message to wakr up the cards manager at init. */
  case object Wake

  /** Message to add a word to this folksonomy. */
  case class FolksonomyWord (sentiment: String, word: String)

  /** Message to inform the client about a change in the folksonomy. */
  case class FolksonomyUpdate (card: String, sentiment: String, action: String, word: String)

  /** Message to test the event mediator. */
  case class TestEvent (data: String)

  /** Message to catch client subscriptions. */
  case class ClientSubscription (event: String, socket: ActorRef)

  case object EndOfCommentData

  case class SentimentMsg (sentiment: String) 

  case class AmountUpdate (card: String, sentiment: String, amounts: Int)

  case class SentimentUpdate (card: String, value: Double)

  case class BarsUpdate (card: String, sentimentBars: Map[String, Double])

  case class Commands (cmds: Array[String])

  case class ScheduleNewCard (name: String)

  case class ScheduleComment (card: String, cmt: String)

  protected[actors] val Publish = DistributedPubSubMediator.Publish

  protected[actors] val Subscribe = DistributedPubSubMediator.Subscribe

  protected[actors] val SubscribeAck = DistributedPubSubMediator.SubscribeAck

  protected[actors] val Unsubscribe = DistributedPubSubMediator.Unsubscribe

  protected[actors] val UnsubscribeAck = DistributedPubSubMediator.UnsubscribeAck
}
