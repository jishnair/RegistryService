lazy val akkaHttpVersion = "10.2.2"
lazy val akkaVersion    = "2.6.10"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.jishnair",
      scalaVersion    := "2.13.3"
    )),
    name := "service-registry",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "com.typesafe.akka" %% "akka-http-spray-json"     % "10.1.7",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test,
      "org.scalactic"     %% "scalactic"                % "3.2.2"         % Test
    )
  )
