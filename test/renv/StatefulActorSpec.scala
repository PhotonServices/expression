package renv

import renv.examples._

import akka.actor._
import akka.testkit.{TestActors, TestKit, ImplicitSender}

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class StatefulActorSpec extends TestKit(ActorSystem("stateful-test"))
with ImplicitSender
with WordSpecLike 
with Matchers 
with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A StatefulActor" should {

    "mutate" in {
      val ref = system.actorOf(Props[StatefulTest], "testing")
      ref ! SumHead(4)
      ref ! Prepend(5)
      ref ! GetState
      expectMsg(State(List(5,5)))
    }
  }
}
