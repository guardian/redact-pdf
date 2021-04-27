package services

import zio.{Has, IO, ZIO}
import _root_.taleo.{Document, Filter, TaleoError, SubmissionResponse}

object ContentTypes {
  val Pdf = "application/pdf"
  val Docx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  val Html = "text/html"
  val Json = "application/json"
}

object Taleo {

  trait Service {
    def submissions(page: Int = 1, filters: List[Filter] = Nil): IO[TaleoError, SubmissionResponse]

    def cv(cvLink: String): IO[String, Document]
  }

  def submissions(page: Int = 1, filters: List[Filter] = Nil): ZIO[Taleo, TaleoError, SubmissionResponse] =
    ZIO.accessM(_.get.submissions(page, filters))

  def cv(cvLink: String): ZIO[Taleo, String, Document] =
    ZIO.accessM(_.get.cv(cvLink))
}