package controllers

import scala.concurrent.Future
import scala.io.Source
import akka.stream.scaladsl.StreamConverters
import play.api.mvc._
import redact.PdfRedactor
import play.api.data._
import play.api.data.Forms._
import java.nio.file.Paths
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import akka.actor.Scheduler
import jobs.{SyncStatus, UpdateSpreadsheet}
import org.apache.pdfbox.pdmodel.PDDocument
import taleo.Authentication
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import taleo.TaleoCredentials
import zio.Runtime


class Application(
  cc: ControllerComponents,
  config: Configuration,
  wsClient: WSClient,
) extends AbstractController(cc) {

  implicit val ec = cc.executionContext

  val taleoUrl = config.get[String]("taleo-url")

  var taleoCredentials = TaleoCredentials(
    sessionId = "",
    jSessionId = "",
    taleoSession = "=",
    csrfToken = ""
  )

  val jsonCredentials: String = {
    val source = Source.fromFile(s"/etc/gu/redact-pdf/credentials.json")
    val credentials = source.mkString
    source.close
    credentials
  }

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
      Redirect(routes.Application.index).flashing(
        "error" -> "Missing file")
    }
  }

  private def splitName(name: String) = name.split(" ").toList.filter(_.nonEmpty)

  private def quoteString(s: String) = "\"" + s + "\""

  def importFromTaleo = Action(parse.multipartFormData) { implicit request =>
    request.body.file("pdf").map { pdf =>
      val candidates = PdfRedactor.candidates(pdf.ref)
      val doc = PDDocument.load(pdf.ref)
      val docs = PdfRedactor.splitCandidates(doc, candidates)
      val uploadedFilename = Paths.get(pdf.filename).getFileName.toString
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
      Redirect(routes.Application.index).flashing(
        "error" -> "Missing file")
    }
  }

  def authenticate = Action.async {
    Runtime.default.unsafeRunToFuture(
      Authentication.authenticate(taleoUrl).fold(
        { error => InternalServerError(error) },
        { credentials =>
          taleoCredentials = credentials
          Ok("Authenticated")
        }
      )
    )
  }

  def sync = Action.async {
    val dependencies =
      (services.PlayConfiguration.sharedDriveConfig(config) to services.SharedDriveLive.impl(jsonCredentials)) and
      (services.PlayConfiguration.spreadsheetConfig(config) to services.SpreadsheetLive.impl(jsonCredentials)) and
      ((
        services.HttpClientLive.impl(wsClient) and
        services.TaleoCredentialsLive.impl(taleoCredentials) and
        services.PlayConfiguration.taleoConfig(config))
        to services.TaleoLive.impl)

    Runtime.default.unsafeRunToFuture(
      UpdateSpreadsheet.sync()
        .provideCustomLayer(dependencies)
        .fold({ error =>
          println(error)
          SyncStatus(error = true, lastIndex = 0)
        }, identity)
    ).future.map({ status => Ok(Json.toJson(status)) })
  }
}
