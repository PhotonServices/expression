/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package stateful

import akka.actor.Actor
import akka.persistence._

trait Stateful[A] {

  type Mutate = PartialFunction[(A, Any), A]

  private var callbacks: List[A => Unit] = Nil

  var state: A

  def mutate: Mutate 

  def mutation (x: Any) = {
    state = mutate(state, x)
    callbacks.foreach(_(state))
  }

  def onMutation (f: A => Unit) = callbacks = f :: callbacks
}

trait StatefulActor[A] extends Stateful[A] with Chainer {

  |<<<| { 
    case x: Any if mutate isDefinedAt (state, x) => 
      mutation(x)
  }
}

object StatefulPersistentActor {
  case object Snapshot
}

trait StatefulPersistentActor[A] 
extends PersistentActor 
with Stateful[A] 
with Chain {

  |<<<| { 
    case StatefulPersistentActor.Snapshot => saveSnapshot(state)
    case x: Any if mutate isDefinedAt (state, x) => 
      persist(x)(mutation)
  }

  val receiveRecover: Receive = {
    case RecoveryCompleted => 
      recoveryCompleted
    case SnapshotOffer(_, snapshot) => 
      state = snapshot.asInstanceOf[A]
    case x: Any => 
      mutation(x)
  }

  val receiveCommand: Receive = chain orElse lastly

  def recoveryCompleted: Unit = {}
}
