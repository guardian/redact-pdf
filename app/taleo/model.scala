package taleo

import ai.x.play.json.Encoders.encoder
import akka.util.ByteString
import play.api.libs.json.{JsPath, Json, JsonValidationError}

object ContentTypes {
  val Pdf = "application/pdf"
  val Docx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  val Html = "text/html"
  val Json = "application/json"
}

case class Filter(
  filterNo: Int,
  filterName: String,
  fieldNo: Int,
  fieldMetafieldPath: Option[String] = None,
  fieldAlias: String,
  fieldType: Option[String] = None,
  filterType: Int,
  filterResNo: Option[String] = None,
  filterResGroupNo: Option[String] = None,
  sectionName: String = "Submissions",
  sectionNo: Int = 3,
  filterOperators: List[Int],
  filterPath: String,
  fieldPath: String,
  defaultEntityValueKey: Int = 0,
  defaultValues: List[Int] = List.empty,
  values: List[Int],
  measurementUnit: Option[String] = None,
  geoLocations: Option[String] = None,
  possibleValues: List[Int] = List.empty,
  selectedByDefault: Boolean = false,
  customFilter: Boolean = false,
  advancedFilter: Boolean = false
)

object Filter {
  implicit val format = ai.x.play.json.Jsonx.formatCaseClass[Filter]
}

object Filters {

  val submissionComplete = Filter(
    filterNo = 18,
    filterName = "Submissions completed",
    fieldNo = 114,
    fieldAlias = "F_114",
    filterType = 99,
    filterOperators = List(2),
    filterPath = "isCompleted",
    fieldPath = "profileInformation,isCompleted",
    values = List(1),
  )

  val lineReview = Filter(
    filterNo = 29,
    filterName = "Status",
    fieldNo = 1005,
    fieldAlias = "F_1005",
    filterType = 5,
    filterOperators = List(10),
    filterPath = "cswLatestStatusNo",
    fieldPath = "cswLatestStatus,name",
    values = List(387)
  )

  val standard = List(submissionComplete, lineReview)
}

case class SubmissionRequest(
  pageNumber: Int = 1,
  pageSize: Int = 15,
  elfContextNo: Int = 3,
  listFormatNo: Int = 5485,
  columnsToSort: List[Map[String, Boolean]] = List.empty,
  groupingEnabled: Boolean = true,
  requisitionOwnershipNumber: String = "-5",
  filterList: List[Filter] = List.empty
)

object SubmissionRequest {
  implicit val format = Json.format[SubmissionRequest]
}

case class ListHeader(
  name: String,
  total: Int,
  complete: Boolean
)

object ListHeader {
  implicit val format = Json.format[ListHeader]
}

case class SubmissionRow(
  contestNumber: String,
  reqTitle: String,
  CSUser_lastName_F_102: String,
  CSUser_firstName_F_101: String,
  status: String,
  CSUser_no_F_104: String,
  ProfileInformation_creationDate_F_1007: String
)

object SubmissionRow {
  implicit val format = Json.format[SubmissionRow]
}

case class SubmissionAttributes(
  Row: SubmissionRow
)

object SubmissionAttributes {
  implicit val format = Json.format[SubmissionAttributes]
}

case class SubmissionLink(
  href: String,
  rel: String,
  view: String
)

object SubmissionLink {
  implicit val format = Json.format[SubmissionLink]
}

case class SubmissionLinks(
  candidateResumeDetailLink: Option[SubmissionLink]
)

object SubmissionLinks {
  implicit val format = Json.format[SubmissionLinks]
}

case class SubmissionItem(
  action: String,
  attributes: SubmissionAttributes,
  links: SubmissionLinks
)

object SubmissionItem {
  implicit val format = Json.format[SubmissionItem]
}

case class SubmissionResponse(
  items: List[SubmissionItem],
  listHeader: ListHeader,
)

object SubmissionResponse {
  implicit val format = Json.format[SubmissionResponse]
}

case class TaleoCredentials(
  sessionId: String,
  jSessionId: String,
  taleoSession: String,
  csrfToken: String
)


sealed trait DocumentType
object PDF extends DocumentType
object DOCX extends DocumentType

case class Document(docType: DocumentType, data: ByteString)

sealed trait TaleoError

case class JsonParsingError(error: Seq[(JsPath, Seq[JsonValidationError])]) extends TaleoError
case object AuthenticationError extends TaleoError
case object RequestFailure extends TaleoError