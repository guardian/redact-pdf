package services

case class SpreadsheetConfig(spreadsheetId: String)

object SpreadsheetConfiguration {
  trait Service {
    val config: SpreadsheetConfig
  }
}

case class TaleoConfig(taleoUrl: String)

object TaleoConfiguration {
  trait Service {
    val config: TaleoConfig
  }
}

case class SharedDriveConfig(folderId: String, driveId: Option[String])

object SharedDriveConfiguration {
  trait Service {
    val config: SharedDriveConfig
  }
}