package config

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import akka.http.scaladsl.model.headers.HttpOrigin
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import exception.{AuthenticationException, AuthorizationException}
import sangria.execution.{ExceptionHandler => EHandler, _}
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import schema.{DBSchema, GraphQLSchema}
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * This object will be in the second layer of architecture, just after HTTP server.
 * Proper HTTP request will be converted into JSON object and sent to this server.
 * GraphQL Server will parse that JSON as GraphQL query, execute it and through HTTP layer send response back to the client.
 * It will also catch GraphQL parsing errors and convert those into the proper HTTP responses.
 */
object GraphQLServer {
  private val dao = DBSchema.createDatabase
  private val meshLoader = DBSchema.createMeshLoader
  val allowedOrigins = HttpOriginMatcher(HttpOrigin("http://localhost:4200"))

  val corsSettings = CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginMatcher.*).withAllowCredentials(true).withAllowGenericHttpRequests(true).withAllowedMethods(Seq(HttpMethods.GET, HttpMethods.POST, HttpMethods.DELETE, HttpMethods.OPTIONS, HttpMethods.PUT))
  def endpoint(requestJSON: JsValue)(implicit ec: ExecutionContext): Route = cors(corsSettings) {
    val JsObject(fields) = requestJSON

    val JsString(query) = fields("query")

    QueryParser.parse(query) match {
      case Success(queryAst) =>
        val operation = fields.get("operationName") collect {
          case JsString(op) => op
        }

        val variables = fields.get("variables") match {
          case Some(obj: JsObject) => obj
          case _ => JsObject.empty
        }
        complete(executeGraphQLQuery(queryAst, operation, variables))
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
    }

  }

  //
  private val ErrorHandler: EHandler = EHandler {
    case (_, AuthenticationException(message)) ⇒ HandledException(message)
    case (_, AuthorizationException(message)) ⇒ HandledException(message)
  }

  private def executeGraphQLQuery(query: Document, operation: Option[String], vars: JsObject)(implicit ec: ExecutionContext) = {
    // 9
    Executor.execute(
      GraphQLSchema.schemaDefinition,
      query,
      MyContext(dao, meshLoader),
      variables = vars,
      operationName = operation,
      deferredResolver = GraphQLSchema.resolver,
      exceptionHandler = ErrorHandler, // Custom exception handler
      // middleware = AuthMiddleware :: Nil // Add custom middleware to ensure authentication,
    ).map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }
}
