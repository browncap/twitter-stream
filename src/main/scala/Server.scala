package com.twitter

import fs2.{Pipe, Stream}
import org.http4s.server.blaze.BlazeServerBuilder
import cats.effect._
import cats.implicits._
import cats.effect.concurrent.Ref
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text

import scala.concurrent.duration._

object Server {

  def serve[F[_]](implicit F: ConcurrentEffect[F], CS: ContextShift[F], T: Timer[F]): Stream[F, ExitCode] = {
    for {
      ref <- Stream.eval(Ref.of[F, Int](0))
      ts = new TwitterStream[F](ref)
      svc = routes(ref).orNotFound
      server <- buildServer(svc)
        .concurrently {
          ts.stream
        }
    } yield server
  }

  private def routes[F[_]](ref: Ref[F, Int])(implicit F: ConcurrentEffect[F], CS: ContextShift[F], T: Timer[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => for {
        count <- ref.get
        resp <- Ok(count)
      } yield resp

      case GET -> Root / "ws" =>
        val to: Stream[F, WebSocketFrame] =
          Stream.awakeEvery[F](1.seconds).flatMap { _ =>
            Stream.eval(ref.get.map(i => Text(i.toString)))
          }
        val from: Pipe[F, WebSocketFrame, Unit] = _.evalMap {
          case _ => F.delay(println(s"Unknown"))
        }

        WebSocketBuilder[F].build(to, from)
    }
  }

  private def buildServer[F[_]](services: HttpApp[F])(implicit F: ConcurrentEffect[F], T: Timer[F]): Stream[F, ExitCode] =
    BlazeServerBuilder[F](F, T)
      .bindHttp(8080, "localhost")
      .withHttpApp(services)
      .serve

}
