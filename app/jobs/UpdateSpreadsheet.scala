package jobs

import redact.PdfRedactor
import sheets.SheetCandidate
import taleo.{DOCX, Document, Filters, PDF, TaleoError}

import java.io.ByteArrayOutputStream
import converters.DocxConverter
import play.api.libs.json.Json
import zio.{IO, ZIO}
import services.{SharedDrive, Spreadsheet, Taleo}
import zio.console.{Console, putStrLn}

case class TaleoCandidate(
  firstName: String,
  lastName: String,
  candidateResumeDetailLink: String,
  reqTitle: String,
  id: String,
)

case class SyncStatus(
  lastIndex: Int,
  added: Int = 0,
  skipped: Int = 0,
  error: Boolean = false
)

object SyncStatus {
  implicit val jf = Json.format[SyncStatus]
}

object UpdateSpreadsheet {

  private def resultsForPage(page: Int) = {
    Taleo.submissions(page, Filters.standard).map { response =>
      for {
        item <- response.items
        candidateResumeDetailLink <- item.links.candidateResumeDetailLink.map(_.href)
      } yield TaleoCandidate(
        firstName = item.attributes.Row.CSUser_firstName_F_101,
        lastName = item.attributes.Row.CSUser_lastName_F_102,
        candidateResumeDetailLink = candidateResumeDetailLink,
        reqTitle = item.attributes.Row.reqTitle,
        id = item.attributes.Row.CSUser_no_F_104
      )
    }
  }

  private def taleoSubmissions = {
    def inner(page: Int = 1, agg: List[TaleoCandidate] = Nil): ZIO[Taleo, TaleoError, List[TaleoCandidate]] = {
      resultsForPage(page).flatMap {
        case Nil => IO.succeed(agg)
        case results => inner(page + 1, agg ++ results)
      }
    }
    inner()
  }

  private def convertToPdf(doc: Document) = (doc.docType match {
    case PDF => IO.succeed(doc.data.toArray)
    case DOCX => DocxConverter.convert(doc.data.toArray)
  }).mapError(_ => "Failed to convert CV to pdf")

  private def storeRedactedCV(candidate: TaleoCandidate) = {
    for {
      cv <- Taleo.cv(candidate.candidateResumeDetailLink)
      pdf <- convertToPdf(cv)
      names = List(candidate.firstName, candidate.lastName)
      bOutput = new ByteArrayOutputStream()
      _ = PdfRedactor.redact(pdf, bOutput, names, hasCoverPage = false)
      result <- SharedDrive.upload(candidate.id, bOutput.toByteArray).mapError(error => error.getMessage)
    } yield result
  }

  private def addCandidate(candidate: TaleoCandidate, state: SyncStatus) = for {
    result <- storeRedactedCV(candidate).tapError(putStrLn(_)).option
    _ <- Spreadsheet.addCandidate(SheetCandidate.fromTaleoCandidate(candidate, state.lastIndex + 1, result)).mapError(error => error.toString)
  } yield state.copy(lastIndex = state.lastIndex + 1, added = state.added + 1)

  private def processCandidate(candidate: TaleoCandidate, state: SyncStatus, sheetCandidates: List[SheetCandidate]) =
    if (!sheetCandidates.exists(_.compare(candidate))) {
      for {
        _ <- putStrLn(s"Adding candidate ${candidate.firstName} ${candidate.lastName} (${candidate.id})")
        result <- addCandidate(candidate, state)
      } yield result
    } else {
      for {
        _ <- putStrLn(s"Skipping existing candidate ${candidate.firstName} ${candidate.lastName} (${candidate.id})")
        result <- ZIO.succeed(state.copy(skipped = state.skipped + 1))
      } yield result
    }

  def sync(): ZIO[Spreadsheet with SharedDrive with Taleo with Console, String, SyncStatus] = for {
    sheetCandidates <- Spreadsheet.candidates.mapError(_.toString)
    taleoCandidates <- taleoSubmissions.mapError(_.toString)
    lastIndex = SyncStatus(sheetCandidates.lastOption.map(_.index).getOrElse(0))
    result <- ZIO.foldLeft(taleoCandidates)(lastIndex) { (state, candidate) =>
      processCandidate(candidate, state, sheetCandidates)
    }
  } yield result
}
