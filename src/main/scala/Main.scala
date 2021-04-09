package com.twitter

import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.oauth1
import org.http4s.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import fs2.Stream
import io.circe.Json
import jawnfs2._

import scala.concurrent.ExecutionContext.global

class TwitterStream[F[_]: ConcurrentEffect : ContextShift](ref: Ref[F, Int]) {
  implicit val f = new io.circe.jawn.CirceSupportParser(None, false).facade

  def sign(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
          (req: Request[F]): F[Request[F]] = {
    val consumer = oauth1.Consumer(consumerKey, consumerSecret)
    val token    = oauth1.Token(accessToken, accessSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  def jsonStream(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
            (req: Request[F]): Stream[F, Json] =
    for {
      client <- BlazeClientBuilder(global).stream
      sr  <- Stream.eval(sign(consumerKey, consumerSecret, accessToken, accessSecret)(req))
      res <- client.stream(sr).flatMap(_.body.chunks.parseJsonStream)
    } yield res

  def stream: Stream[F, Unit] = {
    val req = Request[F](Method.GET, uri"https://stream.twitter.com/1.1/statuses/sample.json")
    val s   = jsonStream("<consumerKey>", "<consumerSecret>", "<accessToken>", "<accessSecret>")(req)
    s.evalMap { _ =>
      ref.update(_ + 1)
    }
  }
}

object Main extends IOApp {
  def run(args: List[String]) =
    Server.serve[IO].compile.drain.as(ExitCode.Success)
}
