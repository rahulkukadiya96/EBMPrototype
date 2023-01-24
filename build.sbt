import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "EBMPrototype",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0",
    libraryDependencies += "org.sangria-graphql" %% "sangria" % sangriaVersion,
    libraryDependencies += "org.sangria-graphql" %% "sangria-spray-json" % "1.0.3",
    libraryDependencies += ("com.typesafe.akka" %% "akka-actor-typed" % akkaVersion).cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("com.typesafe.akka" %% "akka-stream" % akkaVersion).cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("com.typesafe.slick" %% "slick" % "3.4.1").cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("com.typesafe.slick" %% "slick-hikaricp" % "3.4.1").cross(CrossVersion.for3Use2_13),
    libraryDependencies += "org.slf4j" % "slf4j-nop" % "2.0.6",
    libraryDependencies += "com.h2database" % "h2" % "2.1.214",
  )
