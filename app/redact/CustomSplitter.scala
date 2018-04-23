package redact

import org.apache.pdfbox.multipdf.Splitter

class CustomSplitter(splitPoints: List[Int]) extends Splitter {

  override def splitAtPage(pageNumber: Int): Boolean = {
    splitPoints.contains(pageNumber)
  }
}
