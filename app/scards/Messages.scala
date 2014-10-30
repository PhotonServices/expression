package scards.messages {

  case class CardNew (id: String, name: String)

  case class CardDelete (id: String)

  case class FolksonomyWord (sentiment: String, word: String)

  case class FolksonomyUpdate (card: String, sentiment: String, action: String, word: String)

  case class Comment (comment: String)

  case class CommentData (sentiment: String, folksonomies: List[String])

  case class Sentiment (sentiment: String)

  case class AmountUpdate (card: String, sentiment: String, amounts: Int)

  case class SentimentUpdate (card: String, value: Float)

  case class BarsUpdate (card: String, sentimentBars: Map[String, Float])

}
