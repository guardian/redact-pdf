package redact

import scala.jdk.CollectionConverters._
import com.typesafe.config.ConfigFactory

import java.io.{File, OutputStream}
import java.awt.Color

import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.rendering.{ImageType, PDFRenderer}
import play.api.Logger
import scala.collection.Searching.Found

object PdfRedactor {

  val config = ConfigFactory.load()
  
  val enableExactStringMatching = config.getBoolean("redacted-exact-strings.enabled")
  val enableGreedyNameMatching = config.getBoolean("greedy-name-match.enabled")
  val enableNewPageSplittingAndDeletion = config.getBoolean("new-page-split-behaviour.enabled")

  val redactStringsList = config.getStringList("redact.genderedwords.list").asScala.toList
  val commonNames = config.getStringList("redact.petnames.list").asScala.toList

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
      name <- names ++ commonNames
      found <- TextFinder.findString(document, name)
    } yield found

    val regexedNames: List[FoundText] = { 
     if (enableGreedyNameMatching) { 
      names.flatMap(word => TextFinder.findStringsMatchingRegex(document, word))
     } else{
        List.empty[FoundText]
     }
    }
    val redactedWords: List[FoundText] = {
      if (enableExactStringMatching) { 
      redactStringsList.flatMap(word => TextFinder.findString(document, word))
      } else {
        List.empty[FoundText]
      }
    }
    redactFoundText(
      document = document,
      redactions = List(
        foundNames,
        redactedWords,
        TextFinder.findEmail(document),
        TextFinder.findUrl(document),
        TextFinder.findWebsite(document, "github.com"),
        TextFinder.findWebsite(document, "linkedin.com"),
        regexedNames,
      ).flatten
    )

    ImageRedactor.redactImages(document)

    removeFirstPage(document)

    val rasterisedDoc = new PDDocument()
    val renderer = new PDFRenderer(document)

    for (page <- 0 until document.getNumberOfPages) {
      val image = renderer.renderImageWithDPI(page, 300, ImageType.RGB)
      val pdPage = new PDPage(PDRectangle.A4)
      val pdImage = JPEGFactory.createFromImage(rasterisedDoc, image)
      val contentStream = new PDPageContentStream(rasterisedDoc, pdPage)
      contentStream.drawImage(pdImage, 0, 0, PDRectangle.A4.getWidth, PDRectangle.A4.getHeight)
      contentStream.close()
      rasterisedDoc.addPage(pdPage)
    }

    rasterisedDoc.save(destination)
    rasterisedDoc.close()
  }

  def removeFirstPage(document: PDDocument) = if (enableNewPageSplittingAndDeletion) document.removePage(0)

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
