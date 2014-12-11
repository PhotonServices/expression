/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package object renv {
  // TODO: Comment from with which actors this messages work.
  // Stateful
  case object GetState
  case class State (obj: Any)

  // Persistence
  case object Snapshot

  // Reporter
  case object UpdateMe
  case class Report (classification: String, obj: Any)
  
  trait ListMutation
  case class ListAdd (obj: Any) extends ListMutation
  case class ListRemove (obj: Any) extends ListMutation
}
