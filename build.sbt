scalaVersion := "2.13.4"

resolvers += Resolver.sonatypeRepo("snapshots")

val http4sVersion = "0.21.22"
val circeVersion  = "0.13.0"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe"        % http4sVersion,
  "io.circe"   %% "circe-generic"       % circeVersion
)
