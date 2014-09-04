package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.Play.current

import actors.WebSocketRouterActor
import actors.ClientMessage

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def socket = WebSocket.acceptWithActor[ClientMessage, JsValue] { request => out =>
    WebSocketRouterActor.props(out)
  }

}
