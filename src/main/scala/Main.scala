package com.twitter

import cats.effect._

object Main extends IOApp {
  def run(args: List[String]) =
    Server.serve[IO].compile.drain.as(ExitCode.Success)
}
