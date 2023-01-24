package main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.language.postfixOps

object Application extends App {
  val PORT = 8080

  implicit val actorSystem = ActorSystem("graphql-server")
  implicit val materializer = ActorMaterializer()

  import actorSystem.dispatcher

  import scala.concurrent.duration._

  scala.sys.addShutdownHook(() -> shutdown())

  //3
  val route: Route = {
    complete("Hello GraphQL Scala!!!")
  }

  Http().bindAndHandle(route, "0.0.0.0", PORT)
  println(s"open a browser with URL: http://localhost:$PORT")


  def shutdown(): Unit = {
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 30 seconds)
  }
}