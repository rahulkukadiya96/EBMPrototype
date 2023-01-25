package schema

import config.MyContext
import models.Patient
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._

object GraphQLSchema {
  private val patientType = ObjectType[Unit, Patient](
    "PATIENT",
    fields[Unit, Patient](
      Field("id", IntType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("age", IntType, resolve = _.value.age)
    )
  )
  private val ID_ARG_TYPE = Argument("id", IntType)
  private val ID_LIST_ARG_TYPE = Argument("ids", ListInputType(IntType))

  private val queryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field(
        "patientList",
        ListType(patientType),
        resolve = c => c.ctx.dao.patientLst
      ),

      Field(
        "patient",
        OptionType(patientType),
        arguments = ID_ARG_TYPE :: Nil,
        resolve = config => config.ctx.dao.getPatient(config.arg(ID_ARG_TYPE))
      ),

      Field(
        "patients",
        OptionType(ListType(patientType)),
        arguments = List(ID_LIST_ARG_TYPE),
        resolve = config => config.ctx.dao.getPatients(config.arg(ID_LIST_ARG_TYPE))
      )
    )
  )

  val schemaDefinition: Schema[MyContext, Unit] = Schema(queryType)
}
