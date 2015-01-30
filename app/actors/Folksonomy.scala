/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

/** Messages for the [[actors.Folksonomy]] actor. 
 *  Also with a simpler Props builder
 */
object Folksonomy {

  /** Message to add a word to this folksonomy. */
  case class FolksonomyWord (sentiment: String, word: String)

  /** Message to inform the client about a change in the folksonomy. */
  case class FolksonomyUpdate (card: String, sentiment: String, action: String, word: String)

  def props (card: String): Props = Props(new Folksonomy(card))
}

/** Mantains a rank of top words which are talked about
 *  in the comments of a sentiment card.
 */
class Folksonomy (card: String, initData: Scard = Scard("", "")) extends Actor {

  import scalaz.Scalaz._
  import collection.mutable.Set
  import play.api.Play.current

  import akka.contrib.pattern.DistributedPubSubMediator.{
    Subscribe,
    Publish}

  import WebSocketRouter.{
    ClientSubscription}

  import Folksonomy.{
    FolksonomyUpdate,
    FolksonomyWord}

  /** The threshold of the top words, can be configured in
   * /conf/application.conf with the attribute 'sentiment.folksonomy.threshold'
   */
  val threshold = current.configuration.getInt("sentiment.folksonomy.threshold") match {
    case Some(value) => value
    case None => throw new Exception("Expected 'sentiment.folksonomy.threshold' configuration option in the play framework configuration file.")
  }

  /** The most n hitted words, ordered by sentiment or globally. */
  /* Map[Sentiment, Set[Word]](
   *   "global" -> Set(),
   *   "excellent" -> Set(),
   *   "good" -> Set(),
   *   "neutral" -> Set(),
   *   "bad" -> Set(),
   *   "terrible" -> Set())
   */
  val top = initData.folksonomyTop
  
  /** All the words which belong to a sentiment and their accumulation. */
  /* Map[Sentiment, collection.mutable.Map[Word, Hits]](
   *   "excellent" -> collection.mutable.Map(),
   *   "good" -> collection.mutable.Map(),
   *   "neutral" -> collection.mutable.Map(),
   *   "bad" -> collection.mutable.Map(),
   *   "terrible" -> collection.mutable.Map())
   */
  val global = initData.folksonomyGlobal

  /** Returns the global map but flattened to a simpler one with all the words of all the sentiments. */
  def flatGlobal: Map[Word, Hits] =
    global.foldLeft(Map.empty[Word, Hits])(_ |+| _._2.toMap)

  /** This actor subscribes to be notified of any client that subscribes to his events. */
  override def preStart() =
    top.foreach { case (sentiment, set) =>
      Actors.mediator ! Subscribe(s"client-subscription:$card:folksonomy-$sentiment:add", self)
    }

  def addWord (word: Word, sentiment: Sentiment) = {
    global(sentiment)(word) = global(sentiment).getOrElse(word, 0) + 1
    Argument((card, sentiment, word, global(sentiment)(word))) >>= Mongo.scards.addWord
  }

  /** Checks if a word should be promoted to the top hitted words of a sentiment. */
  def checkLocalPromotion (word: Word, sentiment: Sentiment) =
    if (top(sentiment).size < threshold)
      promoteLocal(word, sentiment)
    else if (!isTopLocal(word, sentiment) && global(sentiment)(word) > global(sentiment)(leastHittedLocal(sentiment))) {
      promoteLocal(word, sentiment)
      demoteLocal(leastHittedLocal(sentiment), sentiment)
      Argument((card, sentiment, top(sentiment))) >>= Mongo.scards.setTopWordsForSentiment
    }

  /** Checks if a word should be promoted to the top hitted global words. */
  def checkGlobalPromotion (word: Word) =
    if (top("global").size < threshold)
      promoteGlobal(word)
    else if (!isTopGlobal(word) && flatGlobal(word) > flatGlobal(leastHittedGlobal)) {
      promoteGlobal(word)
      demoteGlobal(leastHittedGlobal)
      Argument((card, "global", top("global"))) >>= Mongo.scards.setTopWordsForSentiment
    }

  /** Returns the least hitted word from the top words of a sentiment. */
  def leastHittedLocal (sentiment: Sentiment): Word =
    top(sentiment).foldLeft(top(sentiment).head) { (x, word) =>
      if (global(sentiment)(word) < global(sentiment)(x)) word
      else x
    }

  /** Returns the least hitted word from the top global. */
  def leastHittedGlobal: Word =
    top("global").foldLeft(top("global").head) { (x, word) =>
      if (flatGlobal(word) < flatGlobal(x)) word
      else x
    }

  /** Returns true if the word is containes in the top hitted words of a sentiment. */
  def isTopLocal (word: Word, sentiment: Sentiment) =
    top(sentiment).contains(word)

  /** Returns true if the word is contained in the top global hitted words. */
  def isTopGlobal (word: Word) =
    top("global").contains(word)

  /** Adds a word to the top hitted words of a sentiment and publishes it. */
  def promoteLocal (word: Word, sentiment: Sentiment) = {
    top(sentiment) += word
    sendUpdate("add", word, sentiment)
  }

  /** Adds a word to the top global hitted words and publishes it. */
  def promoteGlobal (word: Word) = {
    top("global") += word
    sendUpdate("add", word, "global")
  }

  /** Removes a word from the top hitted words of a sentiment and publish it. */
  def demoteLocal (word: Word, sentiment: Sentiment) = {
    top(sentiment) -= word
    sendUpdate("remove", word, sentiment)
  }

  /** Removes a word from the top global hitted words and publishes it. */
  def demoteGlobal (word: Word) = {
    top("global") -= word
    sendUpdate("remove", word, "global")
  }

  /** Builds a [[actors.Folksonomy.FolksonomyUpdate]] message and publishs it. */
  def sendUpdate(action: String, word: Word, sentiment: Sentiment) =
    Actors.mediator ! Publish(s"$card:folksonomy-$sentiment:$action", FolksonomyUpdate(card, sentiment, action, word))

  /** Extracts a sentiment from a folksonomy event string. */
  val SentimentRegExp = """.*folksonomy-(excellent|good|neutral|bad|terrible|global):add""".r

  def receive = {
    case FolksonomyWord(sentiment, word) => 
      addWord(word, sentiment)
      checkLocalPromotion(word, sentiment)
      checkGlobalPromotion(word)

    case ClientSubscription(event, socket) => event match {
      case SentimentRegExp(sentiment)  =>
        top(sentiment) foreach (socket ! FolksonomyUpdate(card, sentiment, "add", _))
    }
  }
}

