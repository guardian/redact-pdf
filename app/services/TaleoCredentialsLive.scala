package services

import zio.{Layer, ZLayer}

object TaleoCredentialsLive {

  def impl(suppliedCredentials: taleo.TaleoCredentials): Layer[Any, TaleoCredentials] = ZLayer.succeed(
    new TaleoCredentials.Service {
      val credentials: taleo.TaleoCredentials = suppliedCredentials
    }
  )
}
