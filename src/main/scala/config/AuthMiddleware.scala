package config

import models.Authorized
import sangria.execution.{Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.{Action, Context}

object AuthMiddleware extends Middleware[MyContext] with MiddlewareBeforeField[MyContext] {
  override type QueryVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[MyContext, _, _]): Unit = ()

  override def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[MyContext, _, _]): Unit = ()

  override type FieldVal = Unit

  override def beforeField(queryVal: QueryVal, mctx: MiddlewareQueryContext[MyContext, _, _], ctx: Context[MyContext, _]): (Unit, Option[Action[MyContext, _]]) = {
    if (ctx.field.tags contains Authorized) {
      ctx.ctx.ensureAuthenticated()
    }
    continue
  }
}
