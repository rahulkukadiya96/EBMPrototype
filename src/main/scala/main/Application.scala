package main

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import scala.io.StdIn
import scala.concurrent.Await

object Application extends App {
  val PORT = "18089"
  /* val PORT = "18089"

   implicit val actorSystem = akka.actor.typed.ActorSystem("TheSystem")

   import actorSystem.dispatcher
   import scala.concurrent.duration._

   scala.sys.addShutdownHook(() -> shutdown())

   val route: Route = get {
     complete("Hello world")
   }
   Http().newServerAt(route, "0.0.0.0", PORT)
   println(s"open a browser with URL: http://localhost:$PORT")

   private def shutdown(): Unit = {
     actorSystem.terminate()
     Await.result(actorSystem.whenTerminated, 30 seconds)
   }*/

  implicit val system = ActorSystem(Behaviors.empty, "my-system")
  implicit val executionContext = system.executionContext

  val route =
    path("") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }

  private val bindingFuture = Http().newServerAt("localhost", PORT).bind(route)

  println(s"Server now online. Please navigate to http://localhost:${PORT}/ \nPress RETURN to stop...")

  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => {
      system.terminate() // and shutdown when done
    })
}