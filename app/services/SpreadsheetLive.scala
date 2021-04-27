package services

import java.io.ByteArrayInputStream
import java.util.Collections
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import com.google.api.services.sheets.v4.model.{UpdateValuesResponse, ValueRange}
import sheets.SheetCandidate
import zio.{IO, Layer, Task, ZIO, ZLayer}

import scala.collection.JavaConverters._
import scala.collection.mutable

object SpreadsheetLive {

  def impl(credentialsJson: String): ZLayer[SpreadsheetConfiguration, Nothing, Spreadsheet] = ZLayer.fromService[SpreadsheetConfiguration.Service, Spreadsheet.Service]( (config) =>
    new Spreadsheet.Service {
      private val APPLICATION_NAME = "CVRedactor"
      private val JSON_FACTORY = JacksonFactory.getDefaultInstance

      private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
      private val credentials = getCredentials(credentialsJson)
      private val service = new Sheets.Builder(
        HTTP_TRANSPORT,
        JSON_FACTORY,
        credentials
      ).setApplicationName(APPLICATION_NAME).build

      val sheetName = "Sheet1"
      val sheetRange = "A1:F50"

      def candidates(): IO[List[String], List[SheetCandidate]] = IO.effect {
        service
          .spreadsheets
          .values
          .get(config.config.spreadsheetId, s"$sheetName!$sheetRange")
          .setValueRenderOption("FORMULA")
          .execute
      }.foldM(
        error => ZIO.fail(List(error.getMessage)),
        { response =>
          val values = Option(response.getValues).fold(mutable.Buffer.empty[java.util.List[AnyRef]])(_.asScala)
          val (allCandidates, allErrors) = values
            .zipWithIndex
            .foldLeft((List.empty[SheetCandidate], List.empty[String])) {
              case ((candidates, errors), (row, index)) if row.size > 0 =>
                SheetCandidate.fromRow(row.asScala.toList, index) match {
                  case Some(candidate) => (candidate :: candidates, errors)
                  case None => (candidates, errors :+ "failed")
                }
              case ((candidates, errors), _) => (candidates, errors)
            }

          if (allErrors.nonEmpty) {
            ZIO.fail(allErrors)
          } else {
            ZIO.succeed(allCandidates.reverse)
          }
        }
      )

      def addCandidate(candidate: SheetCandidate): Task[UpdateValuesResponse] = ZIO.effect {
        val body = new ValueRange().setValues(List(candidate.asRow.map(_.asInstanceOf[AnyRef]).asJava).asJava)
        service
          .spreadsheets
          .values
          .update(config.config.spreadsheetId, s"A${candidate.index + 1}:F${candidate.index + 1}", body)
          .setValueInputOption("USER_ENTERED")
          .execute
      }

      private def getCredentials(credentialsJson: String) = {
        val in = new ByteArrayInputStream(credentialsJson.getBytes)
        GoogleCredential
          .fromStream(in)
          .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS))
      }
    }
  )
}