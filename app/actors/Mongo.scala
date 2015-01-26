package actors

import com.mongodb.casbah.Imports._
import play.api.Play.current

case object MissingDBException extends Exception
case class BadArguments (m: String) extends Exception(m)

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
  val coll: MongoCollection = db match {
    case Some(db) => db("scards")
    case None => null
  }

  def getScards (args: String): Query[List[Scard], BadArguments] = if (coll == null) Result(List()) else args match {
    case "all meta" => 
      Result(coll.find(MongoDBObject(), MongoDBObject("name" -> 1)).map({ scard =>
        Scard(scard("_id").toString, scard("name").toString) 
      }).toList)
    case _ => 
      FailedQuery(BadArguments("for getScards"))
  }
}

class CommentsCollection (val db: Option[MongoDB]) extends Collection {
  val coll: MongoCollection = db match {
    case Some(db) => db("comments")
    case None => null
  }
}

/**
 * Query monad code.
 */

trait Success

case object Succeeded extends Success

case object Failed extends Success

trait Query[A, E] {
  def >>=[B] (f: A => B): Query[B, E]
  def >=>[B] (f: A => Query[B, E]): Query[B, E]
  def liftError[F] (f: E => F): Query[A, F]
}

case class Argument[A, E] (a: A) extends Query[A, E] { 
  def >>=[R] (f: A => R): Query[R, E] = Result(f(a))
  def >=>[R] (f: A => Query[R, E]): Query[R, E] = f(a)
  def liftError[F] (f: E => F): Query[A, F] = Argument(a)
}

case class Result[R, E] (r: R) extends Query[R, E] {
  def >>=[A] (f: R => A): Query[A, E] = Argument(f(r))
  def >=>[A] (f: R => Query[A, E]): Query[A, E] = f(r)
  def liftError[F] (f: E => F): Query[R, F] = Result(r)
}

case class FailedQuery[A, E] (e: E) extends Query[A, E] {
  def >>=[B] (f: A => B): Query[B, E] = FailedQuery(e)
  def >=>[B] (f: A => Query[B, E]): Query[B, E] = FailedQuery(e)
  def liftError[F] (f: E => F): Query[A, F] = FailedQuery(f(e))
}
