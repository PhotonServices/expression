/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import scala.concurrent.duration._
import play.api.libs.json._

import akka.contrib.pattern.DistributedPubSubMediator.{
  Publish,
  Subscribe,
  Unsubscribe}

import akka.actor.{
  PoisonPill,
  ActorSystem,
  ActorRef,
  Actor,
  Props}

import akka.testkit.{ 
  TestActors, 
  TestKit, 
  ImplicitSender}

import org.scalatest.{
  WordSpecLike, 
  Matchers, 
  BeforeAndAfterAll}

import actors.{
  Actors,
  WebSocketRouter,
  SentimentCardsManager,
  SentimentCard}

import WebSocketRouter.{
  ClientIn,
  ClientOut,
  TestEvent}

import SentimentCard.{
  CardNew,
  CardDelete,
  Comment,
  CommentAck}

class ActorsIntegrationSpec (_system: ActorSystem) extends TestKit(_system) 
with ImplicitSender
with WordSpecLike 
with Matchers 
with BeforeAndAfterAll {

  import system.dispatcher
  var router: ActorRef = _
  var manager: ActorRef = _
  var testCard: ActorRef = _
  var testCardId: String = _

  def this() = this(ActorSystem("ActorsIntegrationSpecsSystem"))

  override def beforeAll {
    Actors(system)
    router = system.actorOf(WebSocketRouter.props(self))
    manager = Actors.sentimentCardsManager
    Actors.mediator ! Subscribe("card-new", self)
    receiveOne(200 milliseconds) // SubscribeAck
    Actors.mediator ! Subscribe("card-delete", self)
    receiveOne(200 milliseconds) // SubscribeAck
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A WebSocketRouter" should {

    "echo to the client" in {
      router ! ClientIn("echo", "hello", "world")
      expectMsg(ClientOut("echo", Json.toJson("hello world")))
    }

    "echo intervals to the client" in {
      within (200 milliseconds) {
        router ! ClientIn("echo", "interval", "100")
        expectMsg(ClientOut("echo", Json.toJson("tick")))
        expectMsg(ClientOut("echo", Json.toJson("tick")))
        router ! ClientIn("echo", "interval", "0")
      }
    }

    "send error on invalid echo interval" in {
      router ! ClientIn("echo", "interval", "WRONG")
      expectMsg(ClientOut("error", Json.toJson("Not an integer")))
    }

    "subscribe the client to events" in {
      router ! ClientIn("events", "subscribe", "test")
      expectMsg(ClientOut("subscribe", Json.toJson("test")))
    }

    "redirect events to the client" in {
      Actors.mediator ! Publish("test", TestEvent("message"))
      expectMsg(ClientOut("test", Json.toJson("message")))
    }

    "unsubscribe the client to events" in {
      router ! ClientIn("events", "unsubscribe", "test")
      expectMsg(ClientOut("unsubscribe", Json.toJson("test")))
    }
  }

  "The SentimentCardManager" should {
    
    "create a new SentimentCard" in {
      manager ! CardNew("", "Testing Card")
      val CardNew(id, name) = receiveOne(200 milliseconds)
      testCardId = id
      assert(name == "Testing Card")
    }

    "delete a sentiment card" in {
      manager ! CardDelete(testCardId)
      val CardDelete(id) = receiveOne(200 milliseconds)
      assert(id == testCardId)
    }
  }

  "A SentimentCard" should {
  
    "publish creation" in {
      testCard = system.actorOf(SentimentCard.props("test-id", "test-card"), "test-id")
      expectMsg(CardNew("test-id", "test-card"))
    }

    "receive a comment" in {
      testCard ! Comment("test")
      expectMsg(CommentAck)
    }

    "publish deletion" in {
      testCard ! PoisonPill
      expectMsg(CardDelete("test-id"))
    }

  }
}
