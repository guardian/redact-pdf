package services

import play.api.libs.ws.{BodyWritable, WSResponse}
import zio.{RIO, Task, ZIO}

object HttpClient {

  trait Service {
    def get(url: String, cookies: List[(String, String)] = Nil, headers: List[(String, String)] = Nil): Task[WSResponse]
    def post[T](
      url: String,
      cookies: List[(String, String)] = Nil,
      headers: List[(String, String)] = Nil,
      body: T
    )(implicit evidence: BodyWritable[T]): Task[WSResponse]
  }

  def get(url: String, cookies: List[(String, String)] = Nil, headers: List[(String, String)] = Nil): RIO[HttpClient, WSResponse] =
    ZIO.accessM(_.get.get(url, cookies, headers))

  def post[T](
    url: String,
    cookies: List[(String, String)] = Nil,
    headers: List[(String, String)] = Nil,
    body: T
  )(implicit evidence: BodyWritable[T]): RIO[HttpClient, WSResponse] =
    ZIO.accessM(_.get.post(url, cookies, headers, body))
}