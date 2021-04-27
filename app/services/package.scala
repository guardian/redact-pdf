import zio.Has

package object services {
  type Taleo = Has[Taleo.Service]
  type HttpClient = Has[HttpClient.Service]
  type TaleoCredentials = Has[TaleoCredentials.Service]
  type Spreadsheet = Has[Spreadsheet.Service]
  type SharedDrive = Has[SharedDrive.Service]
  type SpreadsheetConfiguration = Has[SpreadsheetConfiguration.Service]
  type TaleoConfiguration = Has[TaleoConfiguration.Service]
  type SharedDriveConfiguration = Has[SharedDriveConfiguration.Service]
}
