package redact

import scala.jdk.CollectionConverters._

import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.pdmodel.{PDDocument, PDResources}
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

object ImageRedactor {
  def redactImages(document: PDDocument): Unit = {
    document.getPages.iterator().asScala.foreach { page =>
      replaceImageObjects(page.getResources, document)
    }
  }

  private def replaceImageObjects(resources: PDResources, document: PDDocument): Unit = {
    resources.getXObjectNames.iterator().asScala.foreach { name =>
      resources.getXObject(name) match {
        case image: PDImageXObject =>
          resources.put(name, Image.placeholder(image.getWidth, image.getHeight, document))
        case form: PDFormXObject =>
          Option(form.getResources).foreach { resources =>
            replaceImageObjects(resources, document)
          }
      }
    }
  }
}