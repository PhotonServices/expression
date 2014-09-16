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
  SentimentCard,
  SentimentStats,
  Folksonomy}

import WebSocketRouter.{
  ClientIn,
  ClientOut,
  TestEvent}

import SentimentCard.{
  CardNew,
  CardDelete,
  Comment,
  CommentAck}

import SentimentStats.{
  Sentiment,
  SentimentUpdate,
  AmountUpdate,
  BarsUpdate}

import Folksonomy.{
  FolksonomyWord,
  FolksonomyUpdate}

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
  var stats: ActorRef = _
  var folksonomy: ActorRef = _

  def this() = this(ActorSystem("ActorsIntegrationSpecsSystem"))

  def subscribe (event: String) = {
    Actors.mediator ! Subscribe(event, self)
    receiveOne(200 milliseconds) // SubscribeAck
  }

  def unsubscribe (event: String) = {
    Actors.mediator ! Unsubscribe(event, self)
    receiveOne(200 milliseconds) // UnsubscribeAck
  }

  override def beforeAll {
    Actors(system)
    router = system.actorOf(WebSocketRouter.props(self))
    manager = Actors.sentimentCardsManager
    stats = system.actorOf(SentimentStats.props("testid"))
    folksonomy = system.actorOf(Folksonomy.props("testid"))
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
      subscribe("card-new")
      manager ! CardNew("", "Testing Card")
      val CardNew(id, name) = receiveOne(200 milliseconds)
      testCardId = id
      assert(name == "Testing Card")
      unsubscribe("card-new")
    }

    "delete a sentiment card" in {
      subscribe("card-delete")
      manager ! CardDelete(testCardId)
      val CardDelete(id) = receiveOne(200 milliseconds)
      assert(id == testCardId)
      unsubscribe("card-delete")
    }
  }

  "A SentimentCard" should {
  
    "publish creation" in {
      subscribe("card-new")
      testCard = system.actorOf(SentimentCard.props("test-id", "test-card"), "test-id")
      expectMsg(CardNew("test-id", "test-card"))
      unsubscribe("card-new")
    }

    "receive a comment" in {
      testCard ! Comment("test")
      expectMsg(CommentAck)
    }

    "publish deletion" in {
      subscribe("card-delete")
      testCard ! PoisonPill
      expectMsg(CardDelete("test-id"))
      unsubscribe("card-delete")
    }

  }

  "A SentimentStats actor" should {

    "publish correct stats (1)" in {
      subscribe("testid:sentiment-final")
      subscribe("testid:sentiment-bars")
      subscribe("testid:count-total")
      subscribe("testid:count-excellent")
      stats ! Sentiment("excellent")
      within (200 milliseconds) {
        expectMsg(AmountUpdate("testid", "total", 1))
        expectMsg(AmountUpdate("testid", "excellent", 1))
        expectMsg(SentimentUpdate("testid", 2f))
        expectMsg(BarsUpdate("testid", Map(
          "excellent" -> 100f,
          "good" -> 0f,
          "neutral" -> 0f,
          "bad" -> 0f,
          "terrible" -> 0f)))
      }
      unsubscribe("testid:sentiment-final")
      unsubscribe("testid:sentiment-bars")
      unsubscribe("testid:count-total")
      unsubscribe("testid:count-excellent")
    }
  }

  "A Folksonomy actor" should {

    "publish correct folksonomies (1)" in {
      subscribe("testid:folksonomy-total:add")
      subscribe("testid:folksonomy-excellent:add")
      folksonomy ! FolksonomyWord("excellent", "word1")
      expectMsg(FolksonomyUpdate("testid", "excellent", "add", "word1"))
      //expectMsg(FolksonomyUpdate("testid", "global", "add", "word1")
      unsubscribe("testid:folksonomy-total:add")
      unsubscribe("testid:folksonomy-excellent:add")
    }
  }
}
