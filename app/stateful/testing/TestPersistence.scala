package stateful.testing

import stateful._

case class SumHead (amount: Int)
case class Add (num: Int)

class TestPersistence extends StatefulPersistentActor[List[Int]] {
  var state = List(1)

  def persistenceId = "test-persistence"

  def mutate: Mutate = {
    case (x :: xs, SumHead(amount)) => (x + amount) :: xs
    case (xs, Add(x)) => x :: xs
  }

  override def lastly: Receive = {
    case "state" => println(state)
  }
}

