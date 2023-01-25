package schema

import config.MyContext
import models.Patient
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
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
  private val Id = Argument("id", IntType)
  private val Ids = Argument("ids", ListInputType(IntType))


  implicit val patientHasId: HasId[Patient, Int] = HasId[Patient, Int](_.id)
  private val patientListFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPatients(ids)
  )

  val resolver: DeferredResolver[MyContext] = DeferredResolver.fetchers(patientListFetcher)

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
        arguments = Id :: Nil,
        //resolve = config => config.ctx.dao.getPatient(config.arg(Id))
        resolve = config => patientListFetcher.deferOpt(config.arg(Id))
      ),

      Field(
        "patients",
        OptionType(ListType(patientType)),
        arguments = List(Ids),
        resolve = config => patientListFetcher.deferSeq(config.arg(Ids))
      )
    )
  )

  val schemaDefinition: Schema[MyContext, Unit] = Schema(queryType)
}
