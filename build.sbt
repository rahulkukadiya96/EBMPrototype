import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "EBMPrototype",
    //    libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0",
    libraryDependencies += "org.sangria-graphql" %% "sangria" % "2.1.2",
    libraryDependencies += "org.sangria-graphql" %% "sangria-circe" % "1.3.2",
    libraryDependencies += "org.sangria-graphql" %% "sangria-akka-http-core" % "0.0.2",

    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1",
    libraryDependencies += "io.circe" %% "circe-core" % "0.14.1",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.1",
    libraryDependencies += "org.sangria-graphql" %% "sangria-spray-json" % "1.0.3",

    libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.16",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.16",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    libraryDependencies += "ch.megard" %% "akka-http-cors" % "1.0.0",

    libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion,
    libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.6.6",
    libraryDependencies += "com.h2database" % "h2" % "1.4.196",
    libraryDependencies += "org.neo4j.driver" % "neo4j-java-driver" % "1.6.1",
    libraryDependencies += "io.monix" %% "monix" % "3.1.0",

    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "4.3.0",
    libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "4.3.0" classifier "models",
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.1.0",

    // https://mvnrepository.com/artifact/com.typesafe.play/play-json
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.4",

    libraryDependencies += "com.lihaoyi" %% "requests" % "0.7.0",

    libraryDependencies += "com.itextpdf" % "kernel" % "7.2.5",
    libraryDependencies += "com.itextpdf" % "forms" % "7.2.5",

    libraryDependencies += "com.google.guava" % "guava" % "30.1.1-jre",

    libraryDependencies += scalaTest % Test
  )

Revolver.settings
