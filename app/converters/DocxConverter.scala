package converters

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import fr.opensagres.xdocreport.converter.Options
import fr.opensagres.xdocreport.core.document.DocumentKind
import fr.opensagres.xdocreport.converter.ConverterTypeTo
import fr.opensagres.xdocreport.converter.ConverterRegistry
import zio.{IO, Task}

object DocxConverter {

  def convert(input: Array[Byte]): Task[Array[Byte]] = IO.effect {
    val doc = new ByteArrayInputStream(input)
    val pdf = new ByteArrayOutputStream()
    val options = Options.getFrom(DocumentKind.DOCX).to(ConverterTypeTo.PDF)
    val converter = ConverterRegistry.getRegistry.getConverter(options)
    converter.convert(doc, pdf, options)
    pdf.toByteArray
  }
}
