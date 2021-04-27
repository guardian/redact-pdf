package services

import sheets.CVLink
import zio.{RIO, Task, ZIO}


object SharedDrive {

  trait Service {
    def upload(id: String, data: Array[Byte]): Task[CVLink]
  }

  def upload(id: String, data: Array[Byte]): RIO[SharedDrive, CVLink] = ZIO.accessM(_.get.upload(id, data))
}