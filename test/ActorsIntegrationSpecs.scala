/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import scala.concurrent.duration._
import play.api.Play
import play.api.test.FakeApplication
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
  Folksonomy,
  SentimentAPIRequester}

import WebSocketRouter.{
  ClientIn,
  ClientOut,
  TestEvent}

import SentimentCardsManager.{
  CardNew,
  CardDelete}

import SentimentCard.{
  Comment,
  CommentAck,
  CommentData}

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
  var sentimentApi: ActorRef = _

  Play.start(FakeApplication())

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
    router = system.actorOf(WebSocketRouter.props(self))
    manager = Actors.sentimentCardsManager
    stats = system.actorOf(SentimentStats.props("testid"))
    folksonomy = system.actorOf(Folksonomy.props("testid"))
    sentimentApi = system.actorOf(SentimentAPIRequester.props(self))
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

    "publish deletion" in {
      subscribe("card-delete")
      testCard ! PoisonPill
      expectMsg(CardDelete("test-id"))
      unsubscribe("card-delete")
    }
  }

  "A SentimentStats actor" should {

    "calculate and publish final sentiment" in {
      subscribe("testid:sentiment-final")
      stats ! Sentiment("excellent")
      expectMsg(SentimentUpdate("testid", 2f))
      unsubscribe("testid:sentiment-final")
    }

    "calculate and publish total comments" in {
      subscribe("testid:count-total")
      stats ! Sentiment("excellent")
      expectMsg(AmountUpdate("testid", "total", 2))
      unsubscribe("testid:count-total")
    }

    "calculate and publish total comments of specific sentiment" in {
      subscribe("testid:count-excellent")
      stats ! Sentiment("excellent")
      expectMsg(AmountUpdate("testid", "excellent", 3))
      unsubscribe("testid:count-excellent")
    }

    "calculate and publish statistical bars" in {
      subscribe("testid:sentiment-bars")
      stats ! Sentiment("bad")
      expectMsg(BarsUpdate("testid", Map(
        "excellent" -> 75f,
        "good" -> 0f,
        "neutral" -> 0f,
        "bad" -> 25f,
        "terrible" -> 0f)))
      unsubscribe("testid:sentiment-bars")
    }
  }

  "A Folksonomy actor" should {

    "add and publish words to the global folksonomy" in {
      subscribe("testid:folksonomy-global:add")
      for (i <- 1 to 5) {
        folksonomy ! FolksonomyWord("excellent", s"good word$i")
        expectMsg(FolksonomyUpdate("testid", "global", "add", s"good word$i"))
      }
      unsubscribe("testid:folksonomy-global:add")
    }

    "add and publish words to a specific sentiment folksonomy" in {
      subscribe("testid:folksonomy-bad:add")
      for (i <- 1 to 5) {
        folksonomy ! FolksonomyWord("bad", s"word$i")
        expectMsg(FolksonomyUpdate("testid", "bad", "add", s"word$i"))
      }
      unsubscribe("testid:folksonomy-bad:add")
    }

    "publish updates of only new top words" in {
      subscribe("testid:folksonomy-bad:add")
      for (i <- 1 to 4) {
        folksonomy ! FolksonomyWord("bad", s"word$i")
        expectNoMsg(100 milliseconds)
      }
      unsubscribe("testid:folksonomy-bad:add")
    }

    "publish updates of downgraded words" in {
      subscribe("testid:folksonomy-bad:remove")
      folksonomy ! FolksonomyWord("bad", "word6")
      expectNoMsg(100 milliseconds)
      folksonomy ! FolksonomyWord("bad", "word6")
      expectMsg(FolksonomyUpdate("testid", "bad", "remove", "word5"))
      unsubscribe("testid:folksonomy-bad:remove")
    }
  }

  "A SentimentAPIRequester actor" should {

    "request to the sentiment service (needs an active sentiment service)" in {
      sentimentApi ! Comment("El servicio es excelente.")
      expectMsg(CommentData("excellent", List("servicio")))
    }
  }
}
