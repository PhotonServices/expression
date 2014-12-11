package scards

import scards._
import renv._

import scala.concurrent.duration._
import akka.actor._
import akka.testkit.{TestActors, TestKit, ImplicitSender}

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class CardManagerSpec extends TestKit(ActorSystem("scards-test"))
with ImplicitSender
with WordSpecLike 
with Matchers 
with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A CardManager" should {

    val manager: ActorRef = system.actorOf(Props(new TestCardManager(self)), "testing-card-manager")
    val managerId: String = manager.path.toString + ":state"

    "create a new SentimentCard" in {
      manager ! CardNew(Scard("testing", "Testing"))
      fishForMessage(200 milliseconds) {
        case Publish(event, Report(classification, List(ListAdd(List(Scard(id, name)))) ), _) => 
          assert(id == "testing")
          assert(name == "Testing")
          assert(event == managerId && classification == managerId)
          true
        case _ => false
      }
    }

    "deliver created cards on update request" in {
      manager ! UpdateMe
      fishForMessage(200 milliseconds) {
        case Report(managerId, List(Scard("testing", "Testing"))) => true
        case _ => false
      }
    }

    "delete a sentiment card" in {
      manager ! CardDelete(Scard("testing", "Testing"))
      fishForMessage(200 milliseconds) {
        case Publish(event, Report(classification, List(ListRemove(List(Scard(id, name)))) ), _) =>
          assert(id == "testing")
          assert(name == "Testing")
          assert(event == managerId && classification == managerId)
          true
        case _ => false
      }
    }

  }
}
