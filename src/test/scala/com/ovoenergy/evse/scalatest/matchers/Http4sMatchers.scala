package com.ovoenergy.evse.scalatest.matchers

import cats.effect.IO
import org.http4s.{EntityDecoder, Response, Status}
import org.scalatest._
import org.scalatest.matchers._

import scala.reflect.ClassTag
import scala.util.Try

trait Http4sMatchers extends Matchers {

  class BeUnhandledMatcher extends Matcher[Option[Response[IO]]] {
    def apply(maybeResponse: Option[Response[IO]]): MatchResult = {
      val result = maybeResponse.isEmpty
      MatchResult(result, "Request was unhandled", "Request was handled")
    }
  }

  def beUnhandled = new BeUnhandledMatcher


  class HaveStatusMatcher(status: Status) extends Matcher[Option[Response[IO]]] {

    def apply(maybeResponse: Option[Response[IO]]): MatchResult =
      maybeResponse match {
        case Some(response) =>
          val result = response.status === status
          MatchResult(result, s"FAILED: ${response.status} === $status", s"${response.status} === $status")
        case None =>
          MatchResult(false, "Request was unhandled", "Request was handled")
      }
  }

  def haveStatus(status: Status) = new HaveStatusMatcher(status)


  class HaveBodyMatcher[T](body: T)(implicit decoder: EntityDecoder[IO, T]) extends Matcher[Option[Response[IO]]] {

    def apply(maybeResponse: Option[Response[IO]]): MatchResult =
      maybeResponse match {
        case Some(response) =>
          val responseBody = response.as[T].unsafeRunSync()
          val result = response.as[T].unsafeRunSync() === body
          MatchResult(result, s"FAILED: $responseBody === $body", s"$responseBody === $body")
        case None =>
          MatchResult(false, "Request was unhandled", "Request was handled")
      }
  }

  def haveBody[T](body: T)(implicit decoder: EntityDecoder[IO, T]) = new HaveBodyMatcher(body)


  class HaveFormatMatcher[T](implicit decoder: EntityDecoder[IO, T]) extends Matcher[Option[Response[IO]]] {
    def apply(maybeResponse: Option[Response[IO]]): MatchResult = {
      maybeResponse match {
        case Some(response) =>
          val responseParse = Try(response.as[T].unsafeRunSync()).toEither
          val result = responseParse.isRight
          MatchResult(result, s"FAILED: $responseParse.isRight", s"$responseParse.isRight")
        case None =>
          MatchResult(false, "Request was unhandled", "Request was handled")
      }
    }
  }

  def haveFormat[T: ClassTag](implicit decoder: EntityDecoder[IO, T]) = new HaveFormatMatcher
}