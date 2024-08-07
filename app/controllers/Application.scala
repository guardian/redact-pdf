package controllers

import org.apache.pekko.stream.scaladsl.StreamConverters
import play.api.mvc._
import redact.PdfRedactor
import play.api.data._
import play.api.data.Forms._
import play.api.libs.Files
import java.nio.file.Paths
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

import org.apache.pdfbox.pdmodel.PDDocument
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class Application(cc: ControllerComponents) extends AbstractController(cc) {

  val logger = Logger(this.getClass())

  implicit val ec: scala.concurrent.ExecutionContext = cc.executionContext

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  case class UserData(name: String)
  object UserData {
    def unapply(u: UserData): Option[(String)] = Some(u.name)
  }

  val userForm = Form(
    mapping(
      "name" -> text
    )(UserData.apply)(UserData.unapply)
  )

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
    request.body.file("pdf").map { pdf =>
      userForm.bindFromRequest().fold(
        { formWithErrors => BadRequest },
        { userData =>
          val stream = StreamConverters.asOutputStream().mapMaterializedValue { outputStream =>
            Future {
              PdfRedactor.redact(pdf.ref, outputStream, splitName(userData.name))
              outputStream.close()
            }
          }
          val filename = s"${userData.name.filter(_.isLetter)}.pdf"
          Ok.chunked(stream).withHeaders("Content-Disposition" -> s"inline; filename=$filename")
        }
      )
    }.getOrElse {
      Redirect(routes.Application.index()).flashing(
        "error" -> "Missing file")
    }
  }

  private def splitName(name: String) = name.split(" ").toList.filter(_.nonEmpty)

  private def quoteString(s: String) = "\"" + s + "\""


  class ControlledCloseZipOutputStream(os: java.io.OutputStream) extends ZipOutputStream(os) {
    override def close(): Unit = { }
    def closeNow(): Unit = super.close()
  }

  def importFromTaleo: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
    request.body.file("pdf").map { pdf =>
      val candidates = PdfRedactor.candidates(pdf.ref)
      val doc = PDDocument.load(pdf.ref)
      val docs = PdfRedactor.splitCandidates(doc, candidates)
      val uploadedFilename = Paths.get(pdf.filename).getFileName.toString
      val filename = uploadedFilename.replace(".pdf", "-anon.zip")

      val stream = StreamConverters.asOutputStream().mapMaterializedValue { outputStream =>
        Future {
          val zos = new ControlledCloseZipOutputStream(outputStream) 

          docs.foreach { case (candidate, doc) =>
            val entryName = s"anon-candidates/${candidate.id}-redact.pdf"
            zos.putNextEntry(new ZipEntry(entryName))
            try {
              candidate.firstName.split(" ").toList
              PdfRedactor.redact(doc, zos, splitName(candidate.firstName) ++ splitName(candidate.lastName))
              doc.close()
            } catch {
              case e: Exception => logger.error("Oops", e)
            }
            zos.closeEntry()
          }
          doc.close()

          zos.putNextEntry(new ZipEntry("anon-candidates/candidates.csv"))
          candidates.foreach { c =>
            zos.write(List(s"${c.lastName}, ${c.firstName}", c.id, c.jobText, c.jobId).map(quoteString).mkString("", ",", "\n").getBytes)
          }
          zos.closeEntry()
          zos.closeNow()
        }
      }
      Ok.chunked(stream).withHeaders("Content-Disposition" -> s"inline; filename=$filename")
    }.getOrElse {
      Redirect(routes.Application.index()).flashing(
        "error" -> "Missing file")
    }
  }

  def healthcheck = Action {
    logger.info("Responding OK from healthcheck")
    Ok("Ok")
  }
}
