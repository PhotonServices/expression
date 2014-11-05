/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package stateful

import akka.actor.Actor
import akka.persistence._

trait Chain { 

  var chain: Actor.Receive = Actor.emptyBehavior

  def lastly: Actor.Receive = Actor.emptyBehavior

  def |<<<| (link: Actor.Receive) = chain = chain orElse link
}

trait Chainer extends Chain {
  this: Actor =>

  def receive = chain orElse lastly
}

trait Stateful[A] {

  type Mutate = PartialFunction[Tuple2[A, Any], A]

  var state: A

  def mutate: Mutate 

  def mutation (x: Any) = state = mutate(state, x)
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
