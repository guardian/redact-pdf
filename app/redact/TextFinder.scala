package redact

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.{PDFTextStripper, TextPosition}

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

case class FoundText(pageIndex: Int, x1: Float, y1: Float, x2: Float, y2: Float, text: String)

object TextFinder {
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

  def findUrl(document: PDDocument): List[FoundText] = {
    val textFinder = new RegexFinder("""http[s]?:\/\/([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)(\/[^ ]*)?""".r)
    textFinder.getText(document)
    textFinder.locations.result()
  }

  def findMultiple(needle: String, haystack: String): List[Int] =
    s"(\\b|_|\\.)$needle(\\b|_|\\.)".r.findAllMatchIn(haystack).toList.map(_.start)

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
      val last = textPositions.get(end - 1)
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