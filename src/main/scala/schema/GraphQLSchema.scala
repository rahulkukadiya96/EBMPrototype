package schema

import config.MyContext
import models.{LocalDateTimeCoerceViolation, Patient}
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.schema.{Field, ListType, ObjectType}
import sangria.schema._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern

object GraphQLSchema {
  object DateTimeFormatUtil {
    def fromStrToDate(format: DateTimeFormatter, date: String): Option[LocalDateTime] = {
      try {
        Some(LocalDateTime.parse(date, format))
      }
      catch {
        case _: Exception => None
      }
    }

    def fromDateToStr(format: DateTimeFormatter, date: LocalDateTime): Option[String] = {
      try {
        Some(format.format(date))
      }
      catch {
        case _: Exception => None
      }
    }
  }

  /**
   * conversions between custom data type (LocalDateTime) and type Sangria understand and then back again to custom type.
   */
  implicit val GraphQLDateTime: ScalarType[LocalDateTime] = ScalarType[LocalDateTime](
    "LocalDateTime", // Define the name
    coerceOutput = (localDateTime, _) => DateTimeFormatUtil.fromDateToStr(ofPattern("yyyy-MM-dd HH:mm"), localDateTime).getOrElse(LocalDateTimeCoerceViolation.errorMessage),
    coerceInput = {
      case StringValue(dt, _, _) => (DateTimeFormatUtil fromStrToDate(ofPattern("yyyy-MM-dd"), dt)).toRight(LocalDateTimeCoerceViolation)
    },
    coerceUserInput = {
      case s: String => (DateTimeFormatUtil fromStrToDate(ofPattern("yyyy-MM-dd"), s)).toRight(LocalDateTimeCoerceViolation)
      case _ => Left(LocalDateTimeCoerceViolation)
    }
  )

  /*val patientType = deriveObjectType[Unit, Patient](
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )*/

  private val patientType = ObjectType[Unit, Patient](
    "PATIENT",
    fields[Unit, Patient](
      Field("id", IntType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("age", IntType, resolve = _.value.age),
      Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)
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
