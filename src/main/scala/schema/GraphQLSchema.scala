package schema

import config.MyContext
import models._
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, Relation, RelationIds}
import sangria.macros.derive.{AddFields, InputObjectTypeName, Interfaces, ReplaceField, deriveInputObjectType, deriveObjectType}
import sangria.schema._
import sangria.marshalling.sprayJson._
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}
import utility.DateTimeFormatUtil

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import scala.concurrent.ExecutionContext.Implicits.global

object GraphQLSchema {


  private val Id = Argument("id", IntType)
  private val Ids = Argument("ids", ListInputType(IntType))


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

  /**
   * Created localdatetime parsor object
   */
  implicit object DateJsonFormat extends RootJsonFormat[LocalDateTime] {

    private val defaultDateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/YYYY");

    override def write(obj: LocalDateTime) = JsString(defaultDateTimeFormat.format(obj))
    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(s) => LocalDateTime.parse(s, defaultDateTimeFormat)
      case _ => throw DeserializationException("Invalid date format. It should be MM/dd/YYYY format")
    }
  }
  /**
   * Query definition start
   */

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
  lazy private val CCEncounterType: ObjectType[Unit, CCEncounter] = deriveObjectType[Unit, CCEncounter](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    //    ReplaceField("subjectiveId", Field("subject", SubjectiveType, resolve = c => subjectiveFetcher.defer(c.value.subjectiveId)))
  )

  // implicit val ccEncounterHasId: HasId[CCEncounter, Int] = HasId[CCEncounter, Int](_.id)
  //  private val ccEncounterFetcherBySubjectiveRel = Relation[CCEncounter, Int]("bySubjectiveId", l => Seq(l.subjectiveId))
  private val ccEncounterFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getCCEncounter(ids),
    (ctx: MyContext, ids: RelationIds[CCEncounter]) => ctx.dao.getCCEncounterBySubjective(Seq(1, 2))
    //    (ctx: MyContext, ids: RelationIds[CCEncounter]) => ctx.dao.getCCEncounterBySubjective(ids(ccEncounterFetcherBySubjectiveRel))
  )

  /**
   * For the PatientMedicalHistoryTable type
   */
  private lazy val PatientMedicalHistoryType: ObjectType[Unit, PatientMedicalHistory] = deriveObjectType[Unit, PatientMedicalHistory](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    //    ReplaceField("subjectiveId", Field("subject", SubjectiveType, resolve = c => subjectiveFetcher.defer(c.value.subjectiveId)))
  )

  // implicit val patientMedicalHistoryHasId: HasId[PatientMedicalHistory, Int] = HasId[PatientMedicalHistory, Int](_.id)
  //  private val patientMedicalHistoryBySubjectiveRel = Relation[PatientMedicalHistory, Int]("bySubjectiveId", l => Seq(l.subjectiveId))
  private val patientMedicalHistoryFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPatientMedicalHistory(ids),
    (ctx: MyContext, ids: RelationIds[PatientMedicalHistory]) => ctx.dao.getPatientMedicalHistoryBySubjective(Seq(1, 2))
    //  (ctx: MyContext, ids: RelationIds[PatientMedicalHistory]) => ctx.dao.getPatientMedicalHistoryBySubjective(ids(patientMedicalHistoryBySubjectiveRel))
  )

  /**
   * For the CC SubjectiveTable type
   */
  private lazy val SubjectiveType: ObjectType[Unit, Subjective] = deriveObjectType[Unit, Subjective](
    Interfaces(IdentifiableType),
    //    AddFields(Field("pmh", ListType(PatientMedicalHistoryType), resolve = c => patientMedicalHistoryFetcher.deferRelSeq(patientMedicalHistoryBySubjectiveRel, c.value.id))),
    //    AddFields(Field("encounter", ListType(CCEncounterType), resolve = c => ccEncounterFetcher.deferRelSeq(ccEncounterFetcherBySubjectiveRel, c.value.id))),
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
        resolve = c => c.ctx.dao.patientList
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
        OptionType(ListType(SubjectiveType)),
        arguments = List(Ids),
        resolve = config => subjectiveFetcher.deferSeq(config.arg(Ids))
      ),
      Field(
        "ccEncounters",
        OptionType(ListType(CCEncounterType)),
        arguments = List(Ids),
        resolve = config => ccEncounterFetcher.deferSeq(config.arg(Ids))
      ),
      Field(
        "patientMedicalHistories",
        OptionType(ListType(PatientMedicalHistoryType)),
        arguments = List(Ids),
        resolve = config => patientMedicalHistoryFetcher.deferSeq(config.arg(Ids))
      )
    )
  )

  /**
   * Query definition end
   */

  /**
   * Mutation definition start
   */


  private val NameArg = Argument("name", StringType)
  private val AddressArg = Argument("address", StringType)
  private val AgeType = Argument("age", IntType)
  private val EmailType = Argument("email", StringType)
  private val PasswordType = Argument("password", StringType)

  /**
   * Provide FromInput for input classes :: To convert from json to case classes
   */
  implicit val AuthProviderEmailInputType: InputObjectType[AuthProviderEmail] = deriveInputObjectType[AuthProviderEmail](
    InputObjectTypeName("AUTH_PROVIDER_EMAIL")
  )

  private lazy val AuthProviderSignupDataInputType: InputObjectType[AuthProviderSignupData] = deriveInputObjectType[AuthProviderSignupData]()
  implicit val authProviderEmailFormat: RootJsonFormat[AuthProviderEmail] = jsonFormat2(AuthProviderEmail)
  implicit val authProviderSignupDataFormat: RootJsonFormat[AuthProviderSignupData] = jsonFormat1(AuthProviderSignupData)

  private val AuthProviderArg = Argument("authProvider", AuthProviderSignupDataInputType)

  lazy val SubjectiveInputType: InputObjectType[Subjective] = deriveInputObjectType[Subjective](
    InputObjectTypeName("SUBJECTIVE_INPUT TYPE")
  )
  lazy val CCEncInputType: InputObjectType[CCEncounter] = deriveInputObjectType[CCEncounter] (
    InputObjectTypeName("CC_ENC_INPUT TYPE")
  )
  lazy val PatientMedicalHistoryInputType: InputObjectType[PatientMedicalHistory] = deriveInputObjectType[PatientMedicalHistory] (
    InputObjectTypeName("PATIENT_MEDICAL_HISTORY_INPUT TYPE")
  )

  implicit val subjectiveFormat: RootJsonFormat[Subjective] = jsonFormat2(Subjective)
  implicit val ccEncFormat: RootJsonFormat[CCEncounter] = jsonFormat4(CCEncounter)
  implicit val PatientMedicalHistoryFormat: RootJsonFormat[PatientMedicalHistory] = jsonFormat7(PatientMedicalHistory)

  private val SubjectiveArg = Argument("subjective", SubjectiveInputType)
  private val CCEncArg = Argument("ccEnc", CCEncInputType)
  private val PatientMedicalHistoryArg = Argument("patientMedicalHistory", PatientMedicalHistoryInputType)
  private val PatientIdArg = Argument("patientId", IntType)

  private val Mutation = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field("createPatient",
        patientType,
        arguments = NameArg :: AgeType :: AddressArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.createPatient(Patient(0, c.arg(NameArg), c.arg(AgeType), c.arg(AddressArg)))
      ),

      Field("createSubject",
        SubjectiveType,
        arguments = PatientIdArg :: SubjectiveArg :: CCEncArg :: PatientMedicalHistoryArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => {
          val dao = c.ctx.dao
          for {
            ccEncData <- dao.createCCEnc(c.arg(CCEncArg))
            patientMedicalHistoryData <- dao.createPatientMedicalHistory(c.arg(PatientMedicalHistoryArg))
            subjectiveData <- dao.createSubject(c.arg(SubjectiveArg))
            subjectiveNode <- dao.buildRelationForSubjectNode(c.arg(PatientIdArg), subjectiveData.id, patientMedicalHistoryData.id, ccEncData.id)
          } yield subjectiveNode
        }
      ),

      /*Field("Login",
        patientType,
        arguments = EmailType :: PasswordType :: Nil,
        resolve = ctx => UpdateCtx(
          ctx.ctx.login(ctx.arg(EmailType), ctx.arg(PasswordType))) { patient =>
          ctx.ctx.copy(currentUser = Some(patient))
        }
      )*/
    )
  )
  /**
   * Mutation definition end
   */

  val schemaDefinition: Schema[MyContext, Unit] = Schema(queryType, Some(Mutation))
}
