package services

import zio.{RIO, Task, ZIO}

object TaleoCredentials {
  trait Service {
    val credentials: taleo.TaleoCredentials
  }

  val credentials: RIO[TaleoCredentials, taleo.TaleoCredentials] =
    ZIO.accessM(x => Task(x.get.credentials))
}