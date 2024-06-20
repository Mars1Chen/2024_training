ThisBuild / scalaVersion := "2.12.12"

ThisBuild / version := "1.0-SNAPSHOT"
val AkkaVersion = "2.8.5"

lazy val root = (project in file("."))
    .enablePlugins(PlayScala)
    .settings(
        name := """play2.8.0Test""",
        libraryDependencies ++= Seq(
            guice,
            "org.scalatest" %% "scalatest" % "3.2.18" % Test,
            "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
            "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
            "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
            "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
            "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
            "com.typesafe.akka" %% "akka-protobuf-v3" % AkkaVersion,
            "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
            "com.typesafe.play" %% "play" % "2.8.21",
            "com.typesafe.play" %% "play-json" % "2.9.2"
        )
    )