/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

import akka.contrib.pattern.DistributedPubSubMediator.Publish
import scala.concurrent.duration._
import play.api.libs.json._
import akka.actor.{
  ActorSystem,
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

class ActorsIntegrationSpec (_system: ActorSystem) extends TestKit(_system) 
with ImplicitSender
with WordSpecLike 
with Matchers 
with BeforeAndAfterAll {

  def this() = this(ActorSystem("ActorsIntegrationSpecsSystem"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  Actors(system)
  val router = system.actorOf(WebSocketRouter.props(self))

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

}
