package services

import com.google.api.services.sheets.v4.model.UpdateValuesResponse
import sheets.SheetCandidate
import zio.{IO, RIO, Task, ZIO}


object Spreadsheet {

  trait Service {
    def candidates(): IO[List[String], List[SheetCandidate]]

    def addCandidate(candidate: SheetCandidate): Task[UpdateValuesResponse]
  }

  def candidates: ZIO[Spreadsheet, List[String], List[SheetCandidate]] = ZIO.accessM(_.get.candidates())

  def addCandidate(candidate: SheetCandidate): RIO[Spreadsheet, UpdateValuesResponse] = ZIO.accessM(_.get.addCandidate(candidate))
}