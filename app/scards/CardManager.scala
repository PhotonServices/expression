/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package scards

import akka.actor._
import renv._

/** Singleton actor which handles the creation and deletion
 *  of sentiment cards, which are abstracted by a [[actors.SentimentCard]]
 *  actor.
 *
 *  Also it subscribes to the 'client-subscription:card-new' event,
 *  which will notify him of every client which subscribes to the
 *  'card-new' event, for every notification the CardManager
 *  redirects the message to every SentimentCard actor.
 */
sealed abstract class GenCardManager  
extends Stateful[List[Scard]]
with StateReporterActor 
with ActorLogging {
  var state: List[Scard] = Nil  

  def genId: String = java.util.UUID.randomUUID.toString filterNot(_ == '-')

  def createCard (id: String, name: String) =
    context.actorOf(Props(classOf[SentimentCard], id, name, eventbus), id)

  def deleteCard (id: String) = context.child(id) match {
    case Some(child) => child ! PoisonPill
    case None => log.info("Tried to kill {} card but was already dead.", id)
  }

  def mutate: Mutate = {
    case (xs, CardNew(card)) => 
      if (card.id == "")
        createCard(genId, card.name)
      else
        createCard(card.id, card.name)
      card :: xs
    case (xs, CardDelete(card)) =>
      deleteCard(card.id)
      xs diff List(card) 
  }
}

class CardManager (val eventbus: ActorRef) 
extends GenCardManager
with StatefulPersistentActor[List[Scard]] {
  def persistenceId = "card-manager"
}

class TestCardManager (val eventbus: ActorRef)
extends GenCardManager
with StatefulActor[List[Scard]]
