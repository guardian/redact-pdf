package services

import play.api.libs.json.Json
import taleo.{AuthenticationError, DOCX, Document, Filter, JsonParsingError, PDF, RequestFailure, SubmissionRequest, SubmissionResponse, TaleoError}
import zio.{IO, ZIO, ZLayer}

object TaleoLive {

  val impl: ZLayer[HttpClient with TaleoCredentials with TaleoConfiguration, Nothing, Taleo] = ZLayer.fromServices[HttpClient.Service, TaleoCredentials.Service, TaleoConfiguration.Service, Taleo.Service]( (httpClient, credentials, config) =>
      new Taleo.Service {
        val apiBase = s"${config.config.taleoUrl}/enterprise/nfr/restapi/v1"

        val cookies = List(
          "arg_sessionid" -> credentials.credentials.sessionId,
          "JSESSIONID" -> credentials.credentials.jSessionId,
          "taleosession" -> credentials.credentials.taleoSession
        )
        val headers = List("X-TALEO-CSRF-TOKEN" -> credentials.credentials.csrfToken)

        def submissions(page: Int = 1, filters: List[Filter] = Nil): IO[TaleoError, SubmissionResponse] =
          httpClient.post(
            url = s"$apiBase/applications/submisions",
            cookies = cookies,
            headers = headers,
            body = Json.toJson(SubmissionRequest(pageNumber = page, filterList = filters))
          ).foldM(
            e => {
              println(cookies)
              println(headers)
              println(e)
              ZIO.fail(RequestFailure)
            },
            response =>
              response.contentType match {
                case ContentTypes.Html => ZIO.fail(AuthenticationError)
                case ContentTypes.Json => ZIO.fromEither(response.json.validate[SubmissionResponse].asEither).mapError(JsonParsingError.apply)
              }
          )

        def cv(cvLink: String): IO[String, Document] =
          httpClient.get(
            cvLink.replace("&SUIFlag=true", ""),
            cookies = cookies,
            headers = headers
          ).foldM(e =>
            ZIO.fail(""),
            response =>
              response.contentType match {
                case ContentTypes.Pdf => ZIO.succeed(Document(PDF, response.bodyAsBytes))
                case ContentTypes.Docx => ZIO.succeed(Document(DOCX, response.bodyAsBytes))
                case _ => ZIO.fail("Error retrieving CV")
              }
        )
      }
    )
}
