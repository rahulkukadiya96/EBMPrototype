package schema

import config.MyContext
import models.Patient
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._

object GraphQLSchema {
  val PateintType = ObjectType[Unit, Patient](
    "PATIENT",
    fields[Unit, Patient](
      Field("id", IntType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("age", IntType, resolve = _.value.age)
    )
  )

  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("patientList", ListType(PateintType), resolve = c => c.ctx.dao.patientLst)
    )
  )

  val SchemaDefinition = Schema(QueryType)
}
