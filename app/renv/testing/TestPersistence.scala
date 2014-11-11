package renv.testing

import renv._

case class SumHead (amount: Int)
case class Prepend (num: Int)
case object Print

trait StatefulTestActor extends Stateful[List[Int]] with ReceiveChain {
  var state = List(1)
  def mutate: Mutate = { 
    case (x :: xs, SumHead(amount)) => (x + amount) :: xs
    case (xs, Prepend(x)) => x :: xs
  }
  override def lastly = {
    case Print => println(state)
  }
}

class StatefulTest extends StatefulTestActor with StatefulActor[List[Int]]

class PersistenceTest extends StatefulTestActor with StatefulPersistentActor[List[Int]] {
  def persistenceId = "test-persistence"
}
