package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.Play.current

import actors.Actors
import actors.WebSocketRouter
import actors.WebSocketRouter.ClientIn
import actors.WebSocketRouter.ClientOut

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def socket = WebSocket.acceptWithActor[ClientIn, ClientOut] { request => out =>
    WebSocketRouter.props(out, Actors.sentimentCardsManager)
  }

}
