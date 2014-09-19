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
  TestEvent,
  ClientSubscription}

import SentimentCardsManager.{
  CardNew,
  CardDelete}

import SentimentCard.{
  Comment,
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

  Play.start(FakeApplication(additionalConfiguration = Map(
    "sentiment.service" -> "http://localhost:8000/comments",
    "sentiment.folksonomy.threshold" -> 5
  )))

  def this() = this(ActorSystem("ActorsIntegrationSpecsSystem"))

  def subscribe (event: String) = {
    Actors.mediator ! Subscribe(event, self)
    receiveOne(200 milliseconds) // SubscribeAck
  }

  def unsubscribe (event: String) = {
    Actors.mediator ! Unsubscribe(event, self)
    receiveOne(200 milliseconds) // UnsubscribeAck
  }

  def publishClientSub (event: String) = 
      Actors.mediator ! Publish(s"client-subscription:$event", ClientSubscription(event, self))

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

  /** 
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * S1: WebSocketRouter 
   *
   *
   */
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

  /** 
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * S2: SentimentCardsManager
   *
   *
   */
  "The SentimentCardsManager" should {

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

    "deliver all the created cards on 'card-new' subscription" in {
      subscribe("card-new")
      for (i <- 1 to 5)
        manager ! CardNew("", s"card$i")
      receiveN(5, 200 milliseconds) foreach {
        case message: CardNew => // Good
        case _ => fail("Did not receive a CardNew message on cards creation.")
      }
      unsubscribe("card-new")
      publishClientSub("card-new")
      receiveN(5, 200 milliseconds) foreach {
        case message: CardNew => // Good
        case _ => fail("Did not receive a Cardnew message on client subscription.")
      }
    }
  }

  /** 
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * S3: SentimentCard
   *
   *
   */
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

  /** 
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * S4: SentimentStats
   *
   *
   */
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

    "deliver latest sentiment on client subscription" in {
      publishClientSub("testid:sentiment-final")
      expectMsg(SentimentUpdate("testid", 1.25f))
    }

    "deliver latest amount on client subscription" in {
      publishClientSub("testid:count-total")
      expectMsg(AmountUpdate("testid", "total", 4))
    }

    "deliver latest bars on client subscription" in {
      publishClientSub("testid:sentiment-bars")
      expectMsg(BarsUpdate("testid", Map(
        "excellent" -> 75f,
        "good" -> 0f,
        "neutral" -> 0f,
        "bad" -> 25f,
        "terrible" -> 0f)))
    }
  }

  /** 
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * S5: Folksonomy
   *
   *
   */
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

    "deliver latest folksonomies on client subscription" in {
      publishClientSub("testid:folksonomy-bad:add")
      receiveN(5, 200 milliseconds) foreach {
        case FolksonomyUpdate(card, sentiment, action, word) =>
          assert(card == "testid")
          assert(sentiment == "bad")
          assert(action == "add")
          assert(Set("word1", "word2", "word3", "word4", "word6") contains word)
        case _ => 
          fail("Did not receive a FolksonomyUpdate message on client subscription.")
      }
    }
  }

  /** 
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * S6: SentimentAPIRequester
   *
   *
   */
  "A SentimentAPIRequester actor" should {

    "request to the sentiment service (needs an active sentiment service)" ignore {
      sentimentApi ! Comment("El servicio es excelente.")
      expectMsg(CommentData("excellent", List("servicio")))
    }
  }
}
