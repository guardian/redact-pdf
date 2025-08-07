package redact

import java.awt.Color

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

object Image {
  def placeholder(width: Int, height: Int, document: PDDocument): PDImageXObject = {
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = img.createGraphics
    graphics.setBackground(Color.LIGHT_GRAY)
    graphics.setColor(Color.DARK_GRAY)
    graphics.clearRect(0, 0, img.getWidth, img.getHeight())
    graphics.drawRect(0, 0, img.getWidth - 1, img.getHeight - 1)
    graphics.drawLine(0, 0, img.getWidth, img.getHeight)
    graphics.drawLine(img.getWidth, 0, 0, img.getHeight)
    PDImageXObject.createFromByteArray(document, toByteArray(img, format = "png"), "redacted.png")
  }

  private def toByteArray(image: BufferedImage, format: String): Array[Byte] = {
    val out = new ByteArrayOutputStream
    try {
      ImageIO.write(image, format, out)
      out.toByteArray
    } finally if (out != null) out.close()
  }
}
