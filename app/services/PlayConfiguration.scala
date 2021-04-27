package services

import play.api.Configuration
import zio.{Task, ZIO, ZLayer}

object PlayConfiguration {

  def getString(playConfig: Configuration, path: String) = ZIO.effect {
    playConfig.get[String](path)
  }

  def getOptionString(playConfig: Configuration, path: String) = ZIO.effect {
    playConfig.get[Option[String]](path)
  }

  def spreadsheetConfig(playConfig: Configuration): ZLayer[Any, Throwable, SpreadsheetConfiguration] = ZLayer.fromEffect {
    for {
      spreadsheetId <- getString(playConfig, "spreadsheet-id")
    } yield new SpreadsheetConfiguration.Service {
      val config = SpreadsheetConfig(spreadsheetId = spreadsheetId)
    }
  }

  def taleoConfig(playConfig: Configuration): ZLayer[Any, Throwable, TaleoConfiguration] = ZLayer.fromEffect {
    for {
      taleoUrl <- getString(playConfig, "taleo-url")
    } yield new TaleoConfiguration.Service {
      val config = TaleoConfig(taleoUrl = taleoUrl)
    }
  }

  def sharedDriveConfig(playConfig: Configuration): ZLayer[Any, Throwable, SharedDriveConfiguration] = ZLayer.fromEffect {
    for {
      folderId <- getString(playConfig, "folder-id")
      driveId <- getOptionString(playConfig, "drive-id")
    } yield new SharedDriveConfiguration.Service {
      val config = SharedDriveConfig(folderId = folderId, driveId = driveId)
    }
  }
}