package redact

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.{PDFTextStripper, TextPosition}

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import play.api.Logger

import com.typesafe.config.ConfigFactory

case class FoundText(pageIndex: Int, x1: Float, y1: Float, x2: Float, y2: Float, text: String)

object TextFinder {

  def findStringsMatchingRegex(document: PDDocument, needle: String): List[FoundText] = {
    val textFinder = new RegexFinder(s"${needle.toLowerCase()}".r)
    textFinder.getText(document)
    textFinder.locations.result()
  }

  def findString(document: PDDocument, needle: String): List[FoundText] = {
    val textFinder = new TextFinder(needle)
    textFinder.getText(document)
    textFinder.locations.result()
  }

  def findEmail(document: PDDocument): List[FoundText] = {
    val textFinder = new RegexFinder("""([a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+)@([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)""".r)
    textFinder.getText(document)
    textFinder.locations.result()
  }

  def findWebsite(document: PDDocument, domain: String): List[FoundText] = {
    val textFinder = new RegexFinder(s"""$domain(\\/[^ ]*)?""".r)
    textFinder.getText(document)
    textFinder.locations.result()
  }

  def findUrl(document: PDDocument): List[FoundText] = {
    val textFinder = new RegexFinder("""http[s]?:\/\/([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)(\/[^ ]*)?""".r)
    textFinder.getText(document)
    textFinder.locations.result()
  }

  def findMultiple(needle: String, haystack: String): List[Int] =
    s"(\\b|_|\\.)$needle(\\b|_|\\.)".r.findAllMatchIn(haystack).toList.map(_.start)

  def analyse(doc: PDDocument) = {
    val textPrinter = new AnalyseCV()
    textPrinter.getText(doc)
    textPrinter.candidates.result().foldRight((doc.getNumberOfPages, List.empty[Candidate])) {
      case (candidate, (lastPage, candidates)) =>
        (candidate.firstPage -1, candidate.copy(lastPage = lastPage) :: candidates)
    }._2
  }
}

case class Candidate(
  firstName: String,
  lastName: String,
  id: String,
  jobText: String,
  jobId: String,
  firstPage: Int,
  lastPage: Int
)

object AnalyseCV {
  val config = ConfigFactory.load()
  val newPageSplitBehaviour = config.getBoolean("new-page-split-behaviour.enabled")

  //Changing the splitDifference to two means that the first page of the redacted cv has the candidate's name and job applied for,
  //rather than having it pull in the next candidate's unredacted at the bottom
  val firstPageSplitDifference = {
    if (newPageSplitBehaviour) {
      2
    } else {
      1
    }
  }
}
class AnalyseCV() extends PDFTextStripper {

  val candidates: ListBuffer[Candidate] = new ListBuffer

  var potentialCandidate: Option[Candidate] = None

  var slidingWindow: List[String] = Nil

  super.setSortByPosition(true)

  val candidateNameRegex = """^([\p{L} ]*), ([\p{L} ]*) \((\d+)\) applied for job:(.*)$""".r
  val jobRegex = """^(.*) \((.*)\)$""".r

  val fullRegex = """^([\p{L}- ]*), ([\p{L}- ]*) \((\d+)\) applied for job:(.*) \((.*)\)$""".r

  def candidateFromMatch(m: Regex.Match) = {
    Candidate(
      firstName = m.group(2),
      lastName = m.group(1),
      id = m.group(3),
      jobText = m.group(4).trim,
      jobId = "",
      firstPage = getCurrentPageNo - 1,
      lastPage = getCurrentPageNo - 1
    )
  }

  override protected def writeString(text: String, textPositions: java.util.List[TextPosition]): Unit = {

    slidingWindow = text :: slidingWindow.take(2)

    val last3Lines = fullRegex.findFirstMatchIn(slidingWindow.take(3).reverse.mkString(" ")).map(candidateFromMatch)
    val last2Lines = fullRegex.findFirstMatchIn(slidingWindow.take(2).reverse.mkString(" ")).map(candidateFromMatch)
    val last1Line = fullRegex.findFirstMatchIn(slidingWindow.take(1).reverse.mkString(" ")).map(candidateFromMatch)


    (last3Lines orElse last2Lines orElse last1Line, potentialCandidate) match {
      case (Some(newCandidate), _) =>
        potentialCandidate = Some(newCandidate)
      case (_, Some(candidate)) =>
        candidates.append(candidate)
        potentialCandidate = None
      case _ =>
    }
  }
}

class TextFinder(val needle: String) extends PDFTextStripper {

  val locations: ListBuffer[FoundText] = new ListBuffer

  super.setSortByPosition(true)

  override protected def writeString(text: String, textPositions: java.util.List[TextPosition]): Unit = {
    TextFinder.findMultiple(needle.toLowerCase, text.toLowerCase).foreach { index =>
      val first = textPositions.get(index)
      val last = textPositions.get(index + needle.length - 1)
      locations.append(
        FoundText(
          pageIndex = getCurrentPageNo - 1,
          x1 = first.getX,
          y1 = first.getY,
          x2 = last.getX + last.getWidth,
          y2 = last.getY + last.getHeight,
          text
        )
      )
    }
  }
}

class RegexFinder(regex: Regex) extends PDFTextStripper {

  val locations: ListBuffer[FoundText] = new ListBuffer

  super.setSortByPosition(true)

  private def find(haystack: String): List[(Int, Int)] =
    regex.findAllMatchIn(haystack).toList.map({ m => (m.start, m.end) })

  override protected def writeString(text: String, textPositions: java.util.List[TextPosition]): Unit = {
    find(text.toLowerCase).foreach { case (index, end) =>
      val first = textPositions.get(index)
      val last = textPositions.get(Math.min(textPositions.size() - 1, end - 1))
      locations.append(
        FoundText(
          pageIndex = getCurrentPageNo - 1,
          x1 = first.getX,
          y1 = first.getY,
          x2 = last.getX + last.getWidth,
          y2 = last.getY + last.getHeight,
          text
        )
      )
    }
  }
}