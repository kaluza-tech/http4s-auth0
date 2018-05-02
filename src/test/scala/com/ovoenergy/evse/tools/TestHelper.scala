package com.ovoenergy.evse.tools

import cats.effect.IO
import io.circe.Json
import io.circe.parser.parse
import org.http4s._
import org.scalatest.EitherValues

object TestHelper extends EitherValues {
  def asJson(string: String): Json = parse(string).right.value

  implicit class RichHttpService(httpService: HttpService[IO]) {
    def executeRequest[T](method: Method, uri: Uri, body: T)(implicit e: EntityEncoder[IO, T]): Option[Response[IO]] = {
      val request: IO[Request[IO]] = Request[IO](method, uri).withBody(body)
      request.flatMap (
        req => httpService.apply(req).value
      ).unsafeRunSync()
    }

    def executeRequest(method: Method, uri: Uri): Option[Response[IO]] =
      httpService(Request[IO](method, uri)).value.unsafeRunSync()

    def get[T](uri: Uri, body: T)(implicit e: EntityEncoder[IO, T]) : Option[Response[IO]] = executeRequest(Method.GET, uri, body)
    def get(uri: Uri): Option[Response[IO]] = executeRequest(Method.GET, uri)

    def put[T](uri: Uri, body: T)(implicit e: EntityEncoder[IO, T]) : Option[Response[IO]] = executeRequest(Method.PUT, uri, body)
    def put(uri: Uri): Option[Response[IO]] = executeRequest(Method.PUT, uri)

    def post[T](uri: Uri, body: T)(implicit e: EntityEncoder[IO, T]) : Option[Response[IO]] = executeRequest(Method.POST, uri, body)
  }
}
