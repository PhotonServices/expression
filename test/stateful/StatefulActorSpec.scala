package stateful

import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import akka.testkit.{TestActors, TestKit, ImplicitSender}
import akka.testkit.TestActorRef

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class StatefulActorSpec extends TestKit(ActorSystem("stateful-test"))
with ImplicitSender
with WordSpecLike 
with Matchers 
with BeforeAndAfterAll {

  import system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A StatefulActor" should {

    class TestActor extends Actor with Chainer with StatefulActor[Int] {
      var state = 1
      def mutate: Mutate = { 
        case (state, i: Int) => 
          val newState = state * i + 1 
          sender ! newState
          newState
      }
    }

    val ref = TestActorRef(new TestActor)
    val actor = ref.underlyingActor

    "mutate" in {
      val result = (ref ? 4).value.get.get
      assert(actor.state == result)
    }
  }
}
