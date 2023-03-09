package main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.headers.{HttpOrigin, HttpOriginRange}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.javadsl.model.HttpHeaderRange
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.config.{Config, ConfigFactory}
import config.GraphQLServer
import spray.json.JsValue

import scala.concurrent.Await
import scala.language.postfixOps

object Application extends App {
  val PORT = 18089
  val applicationConf: Config = ConfigFactory.load("application.conf")

  implicit val actorSystem = ActorSystem("graphql-server", applicationConf)
  implicit val materializer = ActorMaterializer()

  import actorSystem.dispatcher

  import scala.concurrent.duration._

  scala.sys.addShutdownHook(() -> shutdown())

  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  val corsConfig = applicationConf.getConfig("akka-http-cors")
  //  val cors  = new CORSHandler {}

  val corsSettings = CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginMatcher.*).withAllowCredentials(true).withAllowGenericHttpRequests(true).withAllowedMethods(Seq(HttpMethods.GET, HttpMethods.POST, HttpMethods.DELETE, HttpMethods.OPTIONS, HttpMethods.PUT))


  //3
  val route: Route = {
    cors(corsSettings) {
      (post & path("graphql")) {
        entity(as[JsValue]) { requestJson =>
          GraphQLServer.endpoint(requestJson)
        }
      } ~ {
        getFromResource("graphiql.html")
      }
    }
  }

  Http().bindAndHandle(route, "0.0.0.0", PORT)
  println(s"open a browser with URL: http://localhost:$PORT")


  def shutdown(): Unit = {
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 30 seconds)
  }
}