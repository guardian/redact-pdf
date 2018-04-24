package controllers

import akka.stream.scaladsl.StreamConverters
import play.api.mvc._
import redact.PdfRedactor
import play.api.data._
import play.api.data.Forms._
import java.io.BufferedOutputStream
import java.nio.file.Paths
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

import org.apache.pdfbox.pdmodel.PDDocument
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

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
              PdfRedactor.redact(picture.ref, outputStream, splitName(userData.name))
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

  private def splitName(name: String) = name.split(" ").toList.filter(_.nonEmpty)

  def importFromTaleo = Action(parse.multipartFormData) { implicit request =>
    request.body.file("picture").map { picture =>
      val candidates = PdfRedactor.candidates(picture.ref)
      val doc = PDDocument.load(picture.ref)
      val docs = PdfRedactor.splitCandidates(doc, candidates)
      val uploadedFilename = Paths.get(picture.filename).getFileName.toString
      val filename = uploadedFilename.replace(".pdf", "-anon.zip")

      val stream = StreamConverters.asOutputStream().mapMaterializedValue { outputStream =>
        Future {
          val zos = new ZipOutputStream(outputStream) {
            override def close(): Unit = { }

            def closeNow(): Unit = super.close()
          }

          docs.foreach { case (candidate, doc) =>
            val entryName = s"anon-candidates/${candidate.id}-redact.pdf"
            zos.putNextEntry(new ZipEntry(entryName))
            try {
              candidate.firstName.split(" ").toList
              PdfRedactor.redact(doc, zos, splitName(candidate.firstName) ++ splitName(candidate.lastName))
              doc.close()
            } catch {
              case e: Exception => Logger.error("Oops", e)
            }
            zos.closeEntry()
          }
          doc.close()
          zos.closeNow()
        }
      }
      Ok.chunked(stream).withHeaders("Content-Disposition" -> s"inline; filename=$filename")
    }.getOrElse {
      Redirect(routes.HomeController.index).flashing(
        "error" -> "Missing file")
    }
  }
}
