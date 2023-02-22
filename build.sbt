import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.3"

lazy val root = (project in file("."))
  .settings(
    name := "EBMPrototype",
    //    libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0",
    libraryDependencies += "org.sangria-graphql" %% "sangria" % "1.3.0",
    libraryDependencies += "org.sangria-graphql" %% "sangria-spray-json" % "1.0.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

    libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion,
    libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.6.6",
    libraryDependencies += "com.h2database" % "h2" % "1.4.196",
    libraryDependencies += "org.neo4j.driver" % "neo4j-java-driver" % "1.6.1",
    libraryDependencies += "io.monix" %% "monix" % "3.0.0-RC1",

    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "4.3.0",
    libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "4.3.0" classifier "models",
    libraryDependencies += scalaTest % Test
  )

Revolver.settings
