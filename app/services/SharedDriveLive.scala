package services

import zio.{Has, Layer, Task, ZIO, ZLayer}

import java.io.ByteArrayInputStream
import java.util.Collections
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.model.File
import com.google.api.services.drive.{Drive, DriveScopes}
import sheets.CVLink

object SharedDriveLive {

  def impl(credentialsJson: String): ZLayer[SharedDriveConfiguration, Nothing, SharedDrive] = ZLayer.fromService[SharedDriveConfiguration.Service, SharedDrive.Service]( (config) =>
    new SharedDrive.Service {
      private val APPLICATION_NAME = "CVRedactor"
      private val JSON_FACTORY = JacksonFactory.getDefaultInstance

      private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
      private val credentials = getCredentials(credentialsJson)
      private val service = new Drive.Builder(
        HTTP_TRANSPORT,
        JSON_FACTORY,
        credentials
      ).setApplicationName(APPLICATION_NAME).build

      def upload(id: String, data: Array[Byte]): Task[CVLink] = ZIO.effect {
        val fileMetadata = new File()
        fileMetadata.setName(s"candidate-$id.pdf")
        fileMetadata.setParents(Collections.singletonList(config.config.folderId))
        config.config.driveId.foreach(id => fileMetadata.setTeamDriveId(id))
        val documentId = service
          .files()
          .create(fileMetadata, new InputStreamContent("application/pdf", new ByteArrayInputStream(data)))
          .setFields("id, parents")
          .setSupportsTeamDrives(true)
          .execute()
          .getId
        CVLink(documentId)
      }

      private def getCredentials(credentialsJson: String) = {
        val in = new ByteArrayInputStream(credentialsJson.getBytes)
        GoogleCredential
          .fromStream(in)
          .createScoped(Collections.singleton(DriveScopes.DRIVE))
      }
    }
  )
}