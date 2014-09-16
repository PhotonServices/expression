/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package actors

import akka.actor._

object Folksonomy {

  case class FolksonomyWord (sentiment: String, word: String)

  case class FolksonomyUpdate (card: String, sentiment: String, action: String, word: String)

  def props (card: String): Props = Props(new Folksonomy(card))
}

/** Mantains a rank of top words. */
class Folksonomy (card: String) extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish
  import collection.mutable.{
    Map,
    Set}
  import Folksonomy.{
    FolksonomyUpdate,
    FolksonomyWord}

  type Sentiment = String
  type Word = String
  type Hits = Int

  val top = Map[Sentiment, Set[Word]](
    "global" -> Set(),
    "excellent" -> Set(),
    "good" -> Set(),
    "neutral" -> Set(),
    "bad" -> Set(),
    "terrible" -> Set())

  val global = Map[Sentiment, Map[Word, Hits]](
    "excellent" -> Map(),
    "good" -> Map(),
    "neutral" -> Map(),
    "bad" -> Map(),
    "terrible" -> Map())

  val threshold = 5

  def addWord (word: Word, sentiment: Sentiment) =
    global(sentiment)(word) = global(sentiment).getOrElse(word, 0) + 1

  def checkLocalPromotion (word: Word, sentiment: Sentiment) =
    if (top(sentiment).size < threshold)
      promoteLocal(word, sentiment)
    else if (!isTopLocal(word, sentiment) && top(sentiment)(word) > top(sentiment)(lessHittedLocal(sentiment))) {
      promoteLocal(word, sentiment)
      demoteLocal(lessHittedLocal(sentiment), sentiment)
    }

  def checkGlobalPromotion (word: Word) =
    if (top("global").size < threshold)
      promoteGlobal(word)
    else if (!isTopGlobal(word) && top("global")(word) > top("global")(lessHittedGlobal)) {
      promoteGlobal(word)
      demoteGlobal(lessHittedGlobal)
    }

  def lessHittedLocal (sentiment: Sentiment): Word =
    top(sentiment).foldLeft(top(sentiment).head) { (x, word) =>
      if (global(sentiment)(word) < global(sentiment)(x)) word
      else x
    }

  def lessHittedGlobal: Word =
    top("global").foldLeft(top("global").head) { (x, word) =>
      if (global("global")(word) < global("global")(x)) word
      else x
    }

  def isTopLocal (word: Word, sentiment: Sentiment) =
    top(sentiment).contains(word)

  def isTopGlobal (word: Word) =
    top("global").contains(word)

  def promoteLocal (word: Word, sentiment: Sentiment) = {
    sendUpdate("add", word, sentiment)
    top(sentiment) += word
  }

  def promoteGlobal (word: Word) = {
    sendUpdate("add", word, "global")
    top("global") += word
  }

  def demoteLocal (word: Word, sentiment: Sentiment) = {
    sendUpdate("remove", word, sentiment)
    top(sentiment) -= word
  }

  def demoteGlobal (word: Word) = {
    sendUpdate("remove", word, "global")
    top("global") -= word
  }

  def sendUpdate(action: String, word: Word, sentiment: Sentiment) =
    Actors.mediator ! Publish(s"$card:folksonomy-$sentiment:$action", FolksonomyUpdate (card, sentiment, action, word))

  def receive = {
    case FolksonomyWord(sentiment, word) => 
      addWord(word, sentiment)
      checkLocalPromotion(word, sentiment)
      checkGlobalPromotion(word)
  }
}

