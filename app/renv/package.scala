/**
 * @author Francisco Miguel Arámburo Torres - atfm05@gmail.com
 */

package object renv {
  case object GetState
  case class State (obj: Any)
  case object Snapshot
  case class Report (classification: String, obj: Any)
  case object UpdateMe
}
