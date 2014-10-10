package actors

import spray.httpx.unmarshalling.{MalformedContent, Unmarshaller, Deserialized}
import spray.http._
import spray.json._
import spray.client.pipelining._
import akka.actor.{ActorRef, Actor, Props, Terminated}
import spray.http.HttpRequest
import scala.Some
import domain.{Place, User, Tweet}
import scala.io.Source
import scala.util.Try
import spray.can.Http
import akka.io.IO

object TweetStreamer {
  val twitterUri = Uri("https://stream.twitter.com/1.1/statuses/filter.json")

  def props (processor: ActorRef): Props = 
    Props(new TweetStreamer(twitterUri, processor) with OAuthTwitterAuthorization)
}

class TweetStreamer (uri: Uri, processor: ActorRef) extends Actor with TweetMarshaller {
  this: TwitterAuthorization =>

  import SentimentCard.{
    Comment}

  val io = IO(Http)(context.system)

  override def preStart () = context.watch(processor) 

  def receive: Receive = {
    case query: String =>
      println(s"Will query: $query")
      val body = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`), s"track=$query")
      val request = HttpRequest(HttpMethods.POST, uri = uri, entity = body) ~> authorize
      println(request)
      sendTo(io).withResponsesReceivedBy(self)(request)

    case ChunkedResponseStart(_) =>

    case MessageChunk(entity, _) => TweetUnmarshaller(entity).fold(_ => (), self !)

    case Tweet(id, User(_, "es", _), cmt, _) => 
      println(s"TWEET: $cmt")
      processor ! Comment(cmt)

    case Terminated(_) => context.stop(self)

    case _ =>
  }
}

trait TwitterAuthorization {
  def authorize: HttpRequest => HttpRequest
}

trait OAuthTwitterAuthorization extends TwitterAuthorization {
  import domain.OAuth._
  import play.api.Play.current

  def consumerKey: String = current.configuration.getString("sentiment.twitter.consumer.key") match {
    case Some(string) => string
    case None => throw new Exception("Expected 'sentiment.twitter.consumer.key' configuration option in the play framework configuration file.")
  }

  def consumerSecret: String = current.configuration.getString("sentiment.twitter.consumer.secret") match {
    case Some(string) => string
    case None => throw new Exception("Expected 'sentiment.twitter.consumer.secret' configuration option in the play framework configuration file.")
  }

  def tokenValue: String = current.configuration.getString("sentiment.twitter.token.value") match {
    case Some(string) => string
    case None => throw new Exception("Expected 'sentiment.twitter.token.value' configuration option in the play framework configuration file.")
  }

  def tokenSecret: String = current.configuration.getString("sentiment.twitter.token.secret") match {
    case Some(string) => string
    case None => throw new Exception("Expected 'sentiment.twitter.token.secret' configuration option in the play framework configuration file.")
  }

  val consumer = Consumer(consumerKey, consumerSecret)
  val token = Token(tokenValue, tokenSecret)

  val authorize: (HttpRequest) => HttpRequest = oAuthAuthorizer(consumer, token)
}

trait TweetMarshaller {

  implicit object TweetUnmarshaller extends Unmarshaller[Tweet] {

    def mkUser(user: JsObject): Deserialized[User] = {
      (user.fields("id_str"), user.fields("lang"), user.fields("followers_count")) match {
        case (JsString(id), JsString(lang), JsNumber(followers)) => Right(User(id, lang, followers.toInt))
        case (JsString(id), _, _)                                => Right(User(id, "", 0))
        case _                                                   => Left(MalformedContent("bad user"))
      }
    }

    def mkPlace(place: JsValue): Deserialized[Option[Place]] = place match {
      case JsObject(fields) =>
        (fields.get("country"), fields.get("name")) match {
          case (Some(JsString(country)), Some(JsString(name))) => Right(Some(Place(country, name)))
          case _                                               => Left(MalformedContent("bad place"))
        }
      case JsNull => Right(None)
      case _ => Left(MalformedContent("bad tweet"))
    }

    def apply(entity: HttpEntity): Deserialized[Tweet] = {
      Try {
        val json = JsonParser(entity.asString).asJsObject
        (json.fields.get("id_str"), json.fields.get("text"), json.fields.get("place"), json.fields.get("user")) match {
          case (Some(JsString(id)), Some(JsString(text)), Some(place), Some(user: JsObject)) =>
            val x = mkUser(user).fold(x => Left(x), { user =>
              mkPlace(place).fold(x => Left(x), { place =>
                Right(Tweet(id, user, text, place))
              })
            })
            x
          case _ => Left(MalformedContent("bad tweet"))
        }
      }
    }.getOrElse(Left(MalformedContent("bad json")))
  }
}
