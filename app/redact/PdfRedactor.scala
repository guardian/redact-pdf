package redact

import java.io.{File, OutputStream}

import org.apache.pdfbox.pdmodel.{PDDocument, PDPageContentStream}
import java.awt.Color

import org.apache.pdfbox.multipdf.Splitter

import scala.collection.JavaConverters._
import play.api.Logger


object PdfRedactor {

  def splitCandidates(doc: PDDocument, candidates: List[Candidate]) = {
    val docs = new CustomSplitter(candidates.map(_.firstPage)).split(doc).asScala.toList
    val skipFirstPage = if (candidates.head.firstPage != 0) {
      docs.head.close()
      1
    } else 0
    candidates.zip(docs.drop(skipFirstPage))
  }

  def candidates(source: File): List[Candidate] = {
    val document = PDDocument.load(source)
    val candidates = TextFinder.analyse(document)
    document.close()
    candidates
  }

  def redact(source: File, destination: OutputStream, names: List[String]): Unit = {
    val document = PDDocument.load(source)
    redact(document, destination, names)
    document.close()
  }

  def redact(document: PDDocument, destination: OutputStream, names: List[String]): Unit = {
    val foundNames = for {
      name <- names
      found <- TextFinder.findString(document, name)
    } yield found


    redactFoundText(
      document = document,
      redactions = List(
        foundNames,
        TextFinder.findEmail(document),
        TextFinder.findUrl(document),
        TextFinder.findWebsite(document, "github.com"),
        TextFinder.findWebsite(document, "linkedin.com")
      ).flatten
    )
    document.save(destination)
  }

  def redactFoundText(document: PDDocument, redactions: List[FoundText]): Unit = {
    val allPages = document.getDocumentCatalog.getPages

    redactions.groupBy(_.pageIndex).foreach { case (index, pageData) =>
      val page = allPages.get(index)
      val contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false, true)
      pageData.foreach({ case FoundText(_, x1, y1, x2, y2, _) =>
        contentStream.setNonStrokingColor(Color.BLACK)
        val padding = ((y2 - y1) * 0.3).toFloat
        contentStream.addRect(x1, page.getBBox.getHeight - y1 - padding, x2 - x1, y2 - y1 + padding)
        contentStream.fill()
      })
      contentStream.close()
    }
  }
}

