package sheets
import scala.util.Try
import scala.util.matching.Regex
import jobs.TaleoCandidate


case class CVLink(documentId: String) {
  def url = s"https://drive.google.com/file/d/$documentId/view?usp=sharing"

  def cellValue = s"""=HYPERLINK("${url}", "CV link")"""
}

object CVLink {
  val documentIdPattern: Regex = """https://drive.google.com/file/d/(.*)/view""".r

  def fromCellValue(v: String): Option[CVLink] = {
    documentIdPattern.findFirstMatchIn(v).map { regexMatch =>
      CVLink(regexMatch.group(1))
    }
  }
}
case class SheetCandidate(name: String, id: String, reqTitle: String, cv: Option[CVLink], index: Int) {
  def asRow = List(
    "",
    name,
    id,
    "",
    reqTitle,
    cv.map(_.cellValue).getOrElse("No CV Available"))

  def compare(taleo: TaleoCandidate): Boolean =
    id == taleo.id && reqTitle == taleo.reqTitle
}

object SheetCandidateCols {
  val Name = 1
  val Id = 2
  val ReqTitle = 4
  val cvUrl = 5
}

object SheetCandidate {
  def fromRow(row: List[Any], index: Int): Option[SheetCandidate] = Try {
    SheetCandidate(
      row(SheetCandidateCols.Name).asInstanceOf[String],
      row(SheetCandidateCols.Id) match {
        case v: java.math.BigDecimal => v.toString
        case v: String => v
      },
      row.lift(SheetCandidateCols.ReqTitle).asInstanceOf[Option[String]].getOrElse(""),
      row.lift(SheetCandidateCols.cvUrl).asInstanceOf[Option[String]].filterNot(_ == "No CV Available").flatMap(CVLink.fromCellValue),
      index
    )
  }.toOption

  def fromTaleoCandidate(candidate: TaleoCandidate, index: Int, cv: Option[CVLink]) =
    SheetCandidate(
      s"${candidate.firstName} ${candidate.lastName}",
      candidate.id,
      candidate.reqTitle,
      cv,
      index
    )
}