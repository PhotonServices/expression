/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package scards

import collection.mutable.Set
import scalaz.Scalaz._

import akka.actor._

/** Mantains a rank of top words which are talked about
 *  in the comments of a sentiment card.
 */
class Folksonomy (card: String, eventbus:ActorRef) extends Actor {

  val threshold = 5

  type Sentiment = String
  type Word = String
  type Hits = Int

  /** The most n hitted words, ordered by sentiment or globally. */
  val top = Map[Sentiment, Set[Word]](
    "global" -> Set(),
    "excellent" -> Set(),
    "good" -> Set(),
    "neutral" -> Set(),
    "bad" -> Set(),
    "terrible" -> Set())

  /** All the words which belong to a sentiment and their accumulation. */
  val global = Map[Sentiment, collection.mutable.Map[Word, Hits]](
    "excellent" -> collection.mutable.Map(),
    "good" -> collection.mutable.Map(),
    "neutral" -> collection.mutable.Map(),
    "bad" -> collection.mutable.Map(),
    "terrible" -> collection.mutable.Map())

  /** Returns the global map but flattened to a simpler one with all the words of all the sentiments. */
  def flatGlobal: Map[Word, Hits] =
    global.foldLeft(Map.empty[Word, Hits])(_ |+| _._2.toMap)

  /** This actor subscribes to be notified of any client that subscribes to his events. */
  override def preStart() =
    top.foreach { case (sentiment, set) =>
      eventbus ! Subscribe(s"client-subscription:$card:folksonomy-$sentiment:add", self)
    }

  def addWord (word: Word, sentiment: Sentiment) =
    global(sentiment)(word) = global(sentiment).getOrElse(word, 0) + 1

  /** Checks if a word should be promoted to the top hitted words of a sentiment. */
  def checkLocalPromotion (word: Word, sentiment: Sentiment) =
    if (top(sentiment).size < threshold)
      promoteLocal(word, sentiment)
    else if (!isTopLocal(word, sentiment) && global(sentiment)(word) > global(sentiment)(leastHittedLocal(sentiment))) {
      promoteLocal(word, sentiment)
      demoteLocal(leastHittedLocal(sentiment), sentiment)
    }

  /** Checks if a word should be promoted to the top hitted global words. */
  def checkGlobalPromotion (word: Word) =
    if (top("global").size < threshold)
      promoteGlobal(word)
    else if (!isTopGlobal(word) && flatGlobal(word) > flatGlobal(leastHittedGlobal)) {
      promoteGlobal(word)
      demoteGlobal(leastHittedGlobal)
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

  /** Builds a [[actors.Folksonomy.FolksonomyUpdate]] message and publishs it. */
  def sendUpdate(action: String, word: Word, sentiment: Sentiment) =
    eventbus ! Publish(s"$card:folksonomy-$sentiment:$action", FolksonomyUpdate(card, sentiment, action, word))

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

