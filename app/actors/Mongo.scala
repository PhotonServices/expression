package actors

import collection.mutable.Map
import com.mongodb.casbah.Imports._
import play.api.Play.current

case object MissingDBException extends Exception
case class BadArguments (m: String) extends Exception(m)

case class Scard (id: String, name: String,
  sentimentFinal: Double = 0d,
  sentimentBars: collection.mutable.Map[Sentiment, Double] = Map(
    "excellent" -> 0d,
    "good" -> 0d,
    "neutral" -> 0d,
    "bad" -> 0d,
    "terrible" -> 0d),
  amounts: collection.mutable.Map[Sentiment, Int] = Map(
    "total" -> 0,
    "excellent" -> 0,
    "good" -> 0,
    "neutral" -> 0,
    "bad" -> 0,
    "terrible" -> 0),
  folksonomyTop: collection.immutable.Map[Sentiment, collection.mutable.Set[Word]] = collection.immutable.Map(
    "global" -> collection.mutable.Set.empty,
    "excellent" -> collection.mutable.Set.empty,
    "good" -> collection.mutable.Set.empty,
    "neutral" -> collection.mutable.Set.empty,
    "bad" -> collection.mutable.Set.empty,
    "terrible" -> collection.mutable.Set.empty),
  folksonomyGlobal: collection.immutable.Map[Sentiment, collection.mutable.Map[Word, Hits]] = collection.immutable.Map(
    "excellent" -> Map.empty,
    "good" -> Map.empty,
    "neutral" -> Map.empty,
    "bad" -> Map.empty,
    "terrible" -> Map.empty)
)

case class CommentArchive (
  scardID: String,
  scardName: String,
  comment: String,
  sentiment: String,
  intensity: Int,
  wordCloud: List[String],
  timestamp: Long = System.currentTimeMillis
)

object Mongo {
  
  def confInfo = Tuple3(current.configuration.getString("mongo.host") match {
    case Some(value) => if (value == "") None else Some(value)
    case None => None
  }, current.configuration.getInt("mongo.port") match {
    case Some(value) => if (value == 0) None else Some(value)
    case None => None
  },current.configuration.getString("mongo.db") match {
    case Some(value) => if (value == "") None else Some(value)
    case None => None
  })

  val mongo: Option[MongoDB] = confInfo match {
    case Tuple3(Some(host), Some(port), Some(db)) => Some(MongoClient(host, port)(db))
    case _ => None
  }

  def p[A] (a: A): Unit = println(a.getClass + " :: " + a)

  val scards = new ScardsCollection(mongo)

  val comments = new CommentsCollection(mongo)

  def ifconn[A] (a: A): Query[A, MissingDBException.type] = mongo match {
    case Some(db) => Argument(a)
    case None => FailedQuery(MissingDBException)
  }

  def ifcoll (coll: Option[MongoCollection]): Query[MongoCollection, MissingDBException.type] = coll match {
    case Some(c) => Argument(c)
    case None => FailedQuery(MissingDBException)
  }
}

trait Collection {
  val db: Option[MongoDB]
  val coll: MongoCollection
}

class ScardsCollection (val db: Option[MongoDB]) extends Collection {

  type IMap[A, B] = collection.immutable.Map[A, B]

  val coll: MongoCollection = db match {
    case Some(db) => db("scards")
    case None => null
  }

  def getScards (args: String): Query[List[Scard], BadArguments] = 
    if (coll == null) Result(List()) else args match {
      case "all meta" => 
        Result(coll.find(MongoDBObject(), MongoDBObject("name" -> 1)).map({ scard =>
          Scard(scard("_id").toString, scard("name").toString) 
        }).toList)
      case _ => 
        FailedQuery(BadArguments("for getScards"))
  }

  def getScard (empty: Scard): Query[Scard, Unit] =
    if (coll == null) Result(Scard(empty.id, empty.name)) else {
      Result(coll.findOne(MongoDBObject("_id" -> empty.id)) match {
        case Some(scard) => Scard(empty.id, empty.name,
            sentimentFinal = scard.getAs[Double]("sentiment_final").get,
            sentimentBars = Map(scard.getAs[IMap[Sentiment, Double]]("sentiment_bars").get.toSeq:_*),
            amounts = Map(scard.getAs[IMap[Sentiment, Int]]("amounts").get.toSeq:_*),
            folksonomyTop = scard.getAs[IMap[Sentiment, com.mongodb.BasicDBList]]("folksonomy_top").get map { 
              m => (m._1, collection.mutable.Set((m._2 map { x => x.toString }).toSeq:_*))},
            folksonomyGlobal = scard.getAs[IMap[Sentiment, com.mongodb.BasicDBObject]]("folksonomy_global").get map {
              m => (m._1, Map((m._2 map {x => (x._1, x._2.asInstanceOf[Int]) }).toSeq:_*))}
          )
        case None => Scard(empty.id, empty.name)
      })
    }

  def createScard (args: Scard): Query[Success, Unit] = 
    if (coll == null) Result(Failed) else {
      coll.insert(MongoDBObject(
        "_id" -> args.id, 
        "name" -> args.name,
        "sentiment_final" -> 1.0d,
        "sentiment_bars" -> MongoDBObject(
          "excellent" -> 0.0d,
          "good" -> 0.0d,
          "neutral" -> 0.0d,
          "bad" -> 0.0d,
          "terrible" -> 0.0d
        ),
        "amounts" -> MongoDBObject(
          "total" -> 0,
          "excellent" -> 0,
          "neutral" -> 0,
          "bad" -> 0,
          "terrible" -> 0
        ),
        "folksonomy_top" -> MongoDBObject(
          "global" -> List(),
          "excellent" -> List(),
          "good" -> List(),
          "neutral" -> List(),
          "bad" -> List(),
          "terrible" -> List()
        ),
        "folksonomy_global" -> MongoDBObject(
          "excellent" -> MongoDBObject(),
          "good" -> MongoDBObject(),
          "neutral" -> MongoDBObject(),
          "bad" -> MongoDBObject(),
          "terrible" -> MongoDBObject()
        )
      ))
      Result(Succeeded)
    }

  def deleteScard (args: Scard): Query[Success, Unit] = 
    if (coll == null) Result(Failed) else {
      coll.remove(MongoDBObject("_id" -> args.id))
      Result(Succeeded)
    }

  def updateSentimentFinal (args: Tuple2[Scard, Double]): Query[Success, Unit] =
    if (coll == null) Result(Succeeded) else {
      val scard = args._1
      val sentiment = args._2
      coll.update(MongoDBObject("_id" -> scard.id), $set("sentiment_final" -> sentiment))
      Result(Succeeded)
    }

  def updateSentimentBars (args: Tuple2[Scard, Map[String, Double]]): Query[Success, Unit] =
    if (coll == null) Result(Succeeded) else {
      val s = args._1
      val m = args._2
      coll.update(MongoDBObject(
        "_id" -> s.id
      ),
      $set("sentiment_bars" -> MongoDBObject(
        "excellent" -> m("excellent"),
        "good" -> m("good"),
        "neutral" -> m("neutral"),
        "bad" -> m("bad"),
        "terrible" -> m("terrible")
      )))
      Result(Succeeded)
    }

  def updateAmounts (args: Tuple2[Scard, Map[String, Int]]): Query[Success, Unit] =
    if (coll == null) Result(Succeeded) else {
      val s = args._1
      val m = args._2
      coll.update(MongoDBObject(
        "_id" -> s.id
      ),
      $set("amounts" -> MongoDBObject(
        "total" -> m("total"),
        "excellent" -> m("excellent"),
        "good" -> m("good"),
        "neutral" -> m("neutral"),
        "bad" -> m("bad"),
        "terrible" -> m("terrible")
      )))
      Result(Succeeded)
    }

  def setTopWordsForSentiment (args: Tuple3[String, Sentiment, collection.mutable.Set[Word]]): Query[Success, Unit] =
    if (coll == null) Result(Succeeded) else {
      coll.update(MongoDBObject("_id" -> args._1), $set("folksonomy_top."+args._2 -> args._3.toList))
      Result(Succeeded)
    }

  def addWord (args: Tuple4[String, Sentiment, Word, Hits]): Query[Success, Unit] =
    if (coll == null) Result(Succeeded) else {
      coll.update(MongoDBObject("_id" -> args._1), $set("folksonomy_global."+args._2+"."+args._3 -> args._4))
      Result(Succeeded)
    }
}

class CommentsCollection (val db: Option[MongoDB]) extends Collection {
  val coll: MongoCollection = db match {
    case Some(db) => db("comments")
    case None => null
  }

  def archive (cmt: CommentArchive): Query[Success, Unit] =
    if (coll == null) Result(Succeeded) else {
      coll.insert(MongoDBObject(
        "_id" -> cmt.scardID,
        "scard_id" -> cmt.scardID,
        "scard_name" -> cmt.scardName,
        "comment" -> cmt.comment,
        "sentiment" -> cmt.sentiment,
        "intensity" -> cmt.intensity,
        "word_cloud" -> cmt.wordCloud,
        "timestamp" -> cmt.timestamp
      ))
      Result(Succeeded)
    }
}

/**
 * Query monad code.
 */

trait Success

case object Succeeded extends Success

case object Failed extends Success

trait Query[A, E] {
  def >=>[B] (f: A => B): Query[B, E]
  def >>=[B] (f: A => Query[B, E]): Query[B, E]
}

case class Argument[A, E] (a: A) extends Query[A, E] { 
  def >=>[R] (f: A => R): Query[R, E] = Result(f(a))
  def >>=[R] (f: A => Query[R, E]): Query[R, E] = f(a)
}

case class Result[R, E] (r: R) extends Query[R, E] {
  def >=>[A] (f: R => A): Query[A, E] = Argument(f(r))
  def >>=[A] (f: R => Query[A, E]): Query[A, E] = f(r)
}

case class FailedQuery[A, E] (e: E) extends Query[A, E] {
  def >=>[B] (f: A => B): Query[B, E] = FailedQuery(e)
  def >>=[B] (f: A => Query[B, E]): Query[B, E] = FailedQuery(e)
}
