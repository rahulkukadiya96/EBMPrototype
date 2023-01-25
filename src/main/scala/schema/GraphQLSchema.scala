package schema

import config.MyContext
import models.{CCEncounter, Identifiable, LocalDateTimeCoerceViolation, Patient, PatientMedicalHistory, Subjective}
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.macros.derive.{Interfaces, ReplaceField, deriveObjectType}
import sangria.schema._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern

object GraphQLSchema {


  private val Id = Argument("id", IntType)
  private val Ids = Argument("ids", ListInputType(IntType))

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

  private val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  private val patientType = deriveObjectType[Unit, Patient](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  // implicit val patientHasId: HasId[Patient, Int] = HasId[Patient, Int](_.id)
  private val patientListFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPatients(ids)
  )

  /**
   * For the CC Encounter type
   */
  private val ccEncounterType = deriveObjectType[Unit, CCEncounter](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  // implicit val ccEncounterHasId: HasId[CCEncounter, Int] = HasId[CCEncounter, Int](_.id)
  private val ccEncounterFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getCCEncounter(ids)
  )

  /**
   * For the PatientMedicalHistoryTable type
   */
  private val patientMedicalHistoryType = deriveObjectType[Unit, PatientMedicalHistory](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  // implicit val patientMedicalHistoryHasId: HasId[PatientMedicalHistory, Int] = HasId[PatientMedicalHistory, Int](_.id)
  private val patientMedicalHistoryFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPatientMedicalHistory(ids)
  )

  /**
   * For the CC SubjectiveTable type
   */
  private val subjectiveType = deriveObjectType[Unit, Subjective](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  // implicit val subjectiveHasId: HasId[Subjective, Int] = HasId[Subjective, Int](_.id)
  private val subjectiveFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getSubject(ids)
  )

  val resolver: DeferredResolver[MyContext] =
    DeferredResolver.fetchers(
      patientListFetcher,
      ccEncounterFetcher,
      patientMedicalHistoryFetcher,
      subjectiveFetcher,
    )

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
      ),
      Field(
        "subjectives",
        OptionType(ListType(subjectiveType)),
        arguments = List(Ids),
        resolve = config => subjectiveFetcher.deferSeq(config.arg(Ids))
      ),
      Field(
        "ccEncounters",
        OptionType(ListType(ccEncounterType)),
        arguments = List(Ids),
        resolve = config => ccEncounterFetcher.deferSeq(config.arg(Ids))
      ),
      Field(
        "patientMedicalHistories",
        OptionType(ListType(patientMedicalHistoryType)),
        arguments = List(Ids),
        resolve = config => patientMedicalHistoryFetcher.deferSeq(config.arg(Ids))
      )
    )
  )

  val schemaDefinition: Schema[MyContext, Unit] = Schema(queryType)
}
