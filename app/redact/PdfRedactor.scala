package redact

import java.io.{File, OutputStream}

import org.apache.pdfbox.pdmodel.{PDDocument, PDPageContentStream}
import java.awt.Color

import play.api.Logger

object PdfRedactor {

  def candidates(source: File): List[Candidate] = {
    val document = PDDocument.load(source)
    val candidates = TextFinder.analyse(document)
    document.close()
    candidates
  }

  def redact(source: File, destination: OutputStream, names: List[String]): Unit = {
    val document = PDDocument.load(source)

    for (name <- names) {
      val foundText = TextFinder.findString(document, name)
      redactFoundText(document, foundText)
    }
    val foundEmails = TextFinder.findEmail(document)
    redactFoundText(document, foundEmails)
    val foundUrls = TextFinder.findUrl(document)
    redactFoundText(document, foundUrls)
    document.save(destination)
    document.close()
  }

  def redactFoundText(document: PDDocument, blocks: List[FoundText]): Unit = {
    val allPages = document.getDocumentCatalog.getPages

    blocks.groupBy(_.pageIndex).foreach { case (index, pageData) =>
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

