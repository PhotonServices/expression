package domain

case class User(id: String, lang: String, followersCount: Int)

case class Place(country: String, name: String)

case class Tweet(id: String, user: User, text: String, place: Option[Place])

