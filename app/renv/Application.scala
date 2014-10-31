package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def health = Action {
    Ok("OK")
  }

  /*
  def socket = WebSocket.acceptWithActor[ClientIn, ClientOut] { request => out =>
    WebSocketRouter.props(out)
  }
  */
}
