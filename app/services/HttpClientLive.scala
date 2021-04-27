package services

import play.api.libs.ws.{BodyWritable, DefaultWSCookie, WSClient, WSResponse}
import zio.{Layer, Task, ZIO, ZLayer}

object HttpClientLive {

  def impl(wsClient: WSClient): Layer[Any, HttpClient] = ZLayer.succeed(
    new HttpClient.Service {
      override def get(url: String, cookies: List[(String, String)], headers: List[(String, String)]): Task[WSResponse] = ZIO.fromFuture {
        implicit ec =>
          wsClient.url(url)
            .addCookies(cookies.map(c => DefaultWSCookie(c._1, c._2)): _*)
            .addHttpHeaders(headers: _*)
            .get()
      }

      override def post[T](url: String, cookies: List[(String, String)], headers: List[(String, String)], body: T)(implicit evidence: BodyWritable[T]): Task[WSResponse] = ZIO.fromFuture {
        implicit ec =>
          wsClient.url(url)
            .addCookies(cookies.map(c => DefaultWSCookie(c._1, c._2)): _*)
            .addHttpHeaders(headers: _*)
            .post(body)
      }
    }
  )
}