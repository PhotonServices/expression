package scards

import scards._

import scala.concurrent.duration._
import akka.actor._
import akka.testkit.{TestActors, TestKit, ImplicitSender}

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class SentimentCardSpec extends TestKit(ActorSystem("scards-test"))
with ImplicitSender
with WordSpecLike 
with Matchers 
with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  var card: ActorRef = null

  "A SentimentCard" ignore {

    "publish creation" in {
//      card = system.actorOf(Props(classOf[SentimentCard], "test-id", "test-card", self), "test-id")
//      fishForMessage(200 milliseconds) {
//        case Publish("card-new", CardNew("test-id", "test-card"), _) => true
//        case _ => false
//      }
    }

    "publish deletion" in {
//      card ! PoisonPill
//      fishForMessage(200 milliseconds) {
//        case Publish("card-delete", CardDelete("test-id"), _) => true 
//        case _ => false
//      }
    }

  }
}
