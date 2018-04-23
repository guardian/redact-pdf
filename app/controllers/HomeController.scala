package controllers

import play.api._
import play.api.mvc._

class HomeController(cc: ControllerComponents) extends AbstractController(cc) {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
}
