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
    val oldstate = state
    state = mutate(state, x)
    callbacks foreach(_(oldstate, state))
  }

  def onMutation (f: (A, A)=>Unit) = callbacks = f :: callbacks
}

trait StatefulActor[A] extends Stateful[A] with Chain {

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
this: Actor =>

  import StatefulPersistentActor.Snapshot

  |<<<| { 
    case Snapshot => saveSnapshot(state)
    case x: Any if mutate isDefinedAt (state, x) => 
      persist(x)(mutation)
  }

  def recoveryCompleted: Unit = {}

  val receiveRecover: Receive = {
    case RecoveryCompleted => recoveryCompleted
    case SnapshotOffer(_, snapshot) => 
      println(s"SNAPSHOT: $snapshot")
      state = snapshot.asInstanceOf[A]
    case x: Any => 
      println(s"JOURNALED: $x")
      mutation(x)
  }

  val receiveCommand: Receive = chain orElse lastly
}
