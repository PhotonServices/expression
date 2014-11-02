package stateful

import org.scalatest.FlatSpec

class StatefulActorSpec extends FlatSpec {

  val actor = new StatefulActor[Int] {
    val initial = 0
    val mutation: Mutation = { 
      case (state, x: Int) => state + x 
    }
  }

  "A Stateful Actor" should "initialize" in {
    assert(actor.get == 0)
  }

  it should "mutate" in {
    actor.mutate(1)
    actor.mutate(4)
    assert(actor.get == 5)
  }
}
