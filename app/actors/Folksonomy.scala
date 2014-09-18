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
class Folksonomy (card: String) extends Actor {

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

  val threshold = current.configuration.getInt("sentiment.folksonomy.threshold") match {
    case Some(value) => value
    case None => throw new Exception("Expected 'sentiment.folksonomy.threshold' configuration option in the play framework configuration file.")
  }

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

  val global = Map[Sentiment, collection.mutable.Map[Word, Hits]](
    "excellent" -> collection.mutable.Map(),
    "good" -> collection.mutable.Map(),
    "neutral" -> collection.mutable.Map(),
    "bad" -> collection.mutable.Map(),
    "terrible" -> collection.mutable.Map())

  def flatGlobal: Map[Word, Hits] =
    global.foldLeft(Map.empty[Word, Hits])(_ |+| _._2.toMap)

  override def preStart() =
    top.foreach { case (sentiment, set) =>
      Actors.mediator ! Subscribe(s"client-subscription:$card:folksonomy-$sentiment:add", self)
    }

  def addWord (word: Word, sentiment: Sentiment) =
    global(sentiment)(word) = global(sentiment).getOrElse(word, 0) + 1

  def checkLocalPromotion (word: Word, sentiment: Sentiment) =
    if (top(sentiment).size < threshold)
      promoteLocal(word, sentiment)
    else if (!isTopLocal(word, sentiment) && global(sentiment)(word) > global(sentiment)(lessHittedLocal(sentiment))) {
      promoteLocal(word, sentiment)
      demoteLocal(lessHittedLocal(sentiment), sentiment)
    }

  def checkGlobalPromotion (word: Word) =
    if (top("global").size < threshold)
      promoteGlobal(word)
    else if (!isTopGlobal(word) && flatGlobal(word) > flatGlobal(lessHittedGlobal)) {
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
      if (flatGlobal(word) < flatGlobal(x)) word
      else x
    }

  def isTopLocal (word: Word, sentiment: Sentiment) =
    top(sentiment).contains(word)

  def isTopGlobal (word: Word) =
    top("global").contains(word)

  def promoteLocal (word: Word, sentiment: Sentiment) = {
    top(sentiment) += word
    sendUpdate("add", word, sentiment)
  }

  def promoteGlobal (word: Word) = {
    top("global") += word
    sendUpdate("add", word, "global")
  }

  def demoteLocal (word: Word, sentiment: Sentiment) = {
    top(sentiment) -= word
    sendUpdate("remove", word, sentiment)
  }

  def demoteGlobal (word: Word) = {
    top("global") -= word
    sendUpdate("remove", word, "global")
  }

  def sendUpdate(action: String, word: Word, sentiment: Sentiment) =
    Actors.mediator ! Publish(s"$card:folksonomy-$sentiment:$action", FolksonomyUpdate(card, sentiment, action, word))

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

