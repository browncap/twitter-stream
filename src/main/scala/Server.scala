package com.twitter

import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import cats.effect._
import cats.implicits._
import cats.effect.concurrent.Ref
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._

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
    }
  }

  private def buildServer[F[_]](services: HttpApp[F])(implicit F: ConcurrentEffect[F], T: Timer[F]): Stream[F, ExitCode] =
    BlazeServerBuilder[F](F, T)
      .bindHttp(8080, "localhost")
      .withHttpApp(services)
      .serve

}
