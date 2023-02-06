// This plugin is responsible for restarting server every time you save the files
addSbtPlugin(("io.spray" % "sbt-revolver" % "0.9.1").cross(CrossVersion.for3Use2_13))
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.0")