package controllers

import java.nio.file.Paths

import akka.stream.scaladsl.StreamConverters
import play.api._
import play.api.mvc._
import redact.PdfRedactor
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class HomeController(cc: ControllerComponents, executionContext: ExecutionContext) extends AbstractController(cc) {

  implicit val ec = executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  case class UserData(name: String)

  val userForm = Form(
    mapping(
      "name" -> text
    )(UserData.apply)(UserData.unapply)
  )

  def upload = Action(parse.multipartFormData) { implicit request =>
    request.body.file("picture").map { picture =>
      userForm.bindFromRequest().fold(
        { formWithErrors => BadRequest },
        { userData =>
          val stream = StreamConverters.asOutputStream().mapMaterializedValue { outputStream =>
            Future {
              val res = Try { PdfRedactor.redact(picture.ref, outputStream, List(userData.name)) }
              res match {
                case Success(_) => Logger.info("It worked")
                case Failure(e) => Logger.info("It failed", e)
              }
              outputStream.close()
            }
          }
          val filename = s"${userData.name.filter(_.isLetter)}.pdf"
          Ok.chunked(stream).withHeaders("Content-Disposition" -> s"inline; filename=$filename")
        }
      )


    }.getOrElse {
      Redirect(routes.HomeController.index).flashing(
        "error" -> "Missing file")
    }
  }
}
