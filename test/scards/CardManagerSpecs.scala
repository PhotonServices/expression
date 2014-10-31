import scards._

import scala.concurrent.duration._
import akka.actor._
import akka.testkit.{TestActors, TestKit, ImplicitSender}

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class CardManagerSpec (_system: ActorSystem) extends TestKit(_system) 
with ImplicitSender
with WordSpecLike 
with Matchers 
with BeforeAndAfterAll {

  def this() = this(ActorSystem("scardssys"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  var manager: ActorRef = null
  var testid: String = ""

  "A CardManager" should {

    "subscribe to 'client-subscription:card-new' on create" in {
      manager = system.actorOf(Props(classOf[CardManager], self), "testing-cardmanager")
      expectMsg(Subscribe("client-subscription:card-new", manager))
    }

    "create a new SentimentCard" in {
      manager ! CardNew("", "Testing")
      fishForMessage(200 milliseconds) {
        case Publish("card-new", CardNew(id, "Testing"), _) => 
          testid = id
          true
        case _ => false
      }
    }

    "deliver created cards on 'card-new' subscription" in {
      manager ! ClientSubscription("card-new", self)
      expectMsg(CardNew(testid, "Testing"))
    }

    "delete a sentiment card" in {
      manager ! CardDelete(testid)
      fishForMessage(200 milliseconds) {
        case Publish("card-delete", CardDelete(testid), _) =>
          true
        case _ => false
      }
    }

  }
}
