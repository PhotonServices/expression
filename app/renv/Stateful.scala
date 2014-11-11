/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package renv

import akka.actor.Actor
import akka.persistence._

trait Stateful[A] {
  type Mutation = (A, Any)
  type Mutate = PartialFunction[Mutation, A]
  var state: A
  private var callbacks: List[A => Unit] = Nil
  def onMutation (f: A => Unit) = callbacks = f :: callbacks
  def mutate: Mutate 
  def mutation (a: Any) = {
    state = mutate(state, a)
    callbacks.foreach(_(state))
  }
}

trait StatefulActor[A] 
extends Actor 
with Stateful[A] 
with ReceiveChain {
  linkToChain { 
    case GetState => sender ! State(state)
    case a if mutate isDefinedAt (state, a) => mutation(a)
  }
  def receive = closeChain
}

trait StatefulPersistentActor[A] 
extends PersistentActor 
with Stateful[A] 
with ReceiveChain {
  linkToChain { 
    case GetState => sender ! State(state)
    case Snapshot => saveSnapshot(state)
    case a if mutate isDefinedAt (state, a) => 
      persist(a)(mutation)
  }
  val receiveRecover: Receive = {
    case RecoveryCompleted => 
      recoveryCompleted
    case SnapshotOffer(_, snapshot) => 
      state = snapshot.asInstanceOf[A]
    case a => 
      mutation(a)
  }
  val receiveCommand: Receive = closeChain
  def recoveryCompleted: Unit = {}
}
