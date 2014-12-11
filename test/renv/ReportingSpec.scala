package renv

import renv.examples._

import scala.concurrent.duration._
import akka.actor._
import akka.testkit.{TestActors, TestKit, ImplicitSender}
import akka.contrib.pattern.DistributedPubSubMediator.{
  Publish,
  Subscribe
}

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class ReportingSpec extends TestKit(ActorSystem("reporting-test"))
with ImplicitSender
with WordSpecLike 
with Matchers 
with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A StateReporterActor" should {
    
    val ref = system.actorOf(Props(new ReportingTest(self)), "testing")
    val id = s"${ref.path.toString}:state"

    "report" in {
      ref ! SumHead(4)
      fishForMessage(200 milliseconds) {
        case Publish(event, Report(classification, List(ListAdd(List(5)), ListRemove(List(1))) ), _) => 
          assert(event == id && classification == id)
          true
        case _ => false
      }
    }

    "update" in {
      ref ! UpdateMe
      expectMsg(Report(id, List(5)))
    }

  }
}
