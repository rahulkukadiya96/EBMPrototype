package schema

import config.MyContext
import models._
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, Relation, RelationIds}
import sangria.macros.derive._
import sangria.marshalling.sprayJson._
import sangria.schema._
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
   * Created local-datetime parser object
   */
  implicit object DateJsonFormat extends RootJsonFormat[LocalDateTime] {

    private val defaultDateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    override def write(obj: LocalDateTime): JsString = JsString(defaultDateTimeFormat.format(obj))

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

  private val patientListFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPatients(ids)
  )

  /**
   * For the CC Encounter type
   */
  private val subjectByCCEncRel = Relation[SubjectiveNodeData, Int]("byEnc", l => Seq(l.ccEnc.id))
  private val subjectByPMHRel = Relation[SubjectiveNodeData, Int]("byPatientMedicalHistory", l => Seq(l.patientMedicalHistory.id))

  implicit val CCEncounterType: ObjectType[Unit, CCEncounter] = deriveObjectType[Unit, CCEncounter](
    Interfaces(IdentifiableType),
    AddFields(Field("Subjective", ListType(SubjectiveNodeDataType), resolve = c => subjectiveDataEncFetcher.deferRelSeq(subjectByCCEncRel, c.value.id))),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt.get))
  )

  private val ccEncounterFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getCCEncounter(ids)
  )

  /**
   * For the PatientMedicalHistoryTable type
   */
  implicit val PatientMedicalHistoryType: ObjectType[Unit, PatientMedicalHistory] = deriveObjectType[Unit, PatientMedicalHistory](
    Interfaces(IdentifiableType),
    AddFields(Field("Subjective", ListType(SubjectiveNodeDataType), resolve = c => subjectiveDataPmhFetcher.deferRelSeq(subjectByPMHRel, c.value.id))),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt.get))
  )

  private val patientMedicalHistoryFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPatientMedicalHistory(ids)
  )

  implicit val SubjectiveNodeDataType: ObjectType[Unit, SubjectiveNodeData] = deriveObjectType[Unit, SubjectiveNodeData](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt.get))
  )

  // Define the relation fetcher between subject and enc
  private val subjectiveDataEncFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getSubjectiveData(ids),
    (ctx: MyContext, ids: RelationIds[SubjectiveNodeData]) => ctx.dao.getSubjectiveNodeDataByEncId(ids(subjectByCCEncRel)).flatMap(ctx.dao.getSubjectiveData)
  )

  // Define the relation fetcher between subject and patient medical history
  private val subjectiveDataPmhFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getSubjectiveData(ids),
    (ctx: MyContext, ids: RelationIds[SubjectiveNodeData]) => ctx.dao.getSubjectiveNodeDataByPmhId(ids(subjectByPMHRel)).flatMap(ctx.dao.getSubjectiveData)
  )

  private val objectiveDataFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao getObjectiveData ids
  )

  private val assessmentDataFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getAssessmentData(ids)
  )

  private val planDataFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getPlanData(ids)
  )


  implicit val ObjectiveDataType: ObjectType[Unit, Objective] = deriveObjectType[Unit, Objective](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt.get))
  )

  implicit val AssessmentDataType: ObjectType[Unit, Assessment] = deriveObjectType[Unit, Assessment](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt.get))
  )

  implicit val PlanDataType: ObjectType[Unit, Plan] = deriveObjectType[Unit, Plan](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt.get))
  )

  private lazy val PatientSOAPDataType: ObjectType[Unit, PatientSoap] = deriveObjectType[Unit, PatientSoap](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt.get))
  )


  val resolver: DeferredResolver[MyContext] =
    DeferredResolver.fetchers(
      patientListFetcher,
      ccEncounterFetcher,
      patientMedicalHistoryFetcher,
      subjectiveDataEncFetcher,
      subjectiveDataPmhFetcher,
      objectiveDataFetcher,
      assessmentDataFetcher,
      planDataFetcher
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
        "subjectiveList",
        OptionType(ListType(SubjectiveNodeDataType)),
        resolve = c => c.ctx.dao.getSubjectiveList
      ),
      Field(
        "subjectives",
        OptionType(ListType(SubjectiveNodeDataType)),
        arguments = List(Ids),
        resolve = config => subjectiveDataEncFetcher.deferSeq(config.arg(Ids))
      ),
      Field(
        "ccEncounterList",
        OptionType(ListType(CCEncounterType)),
        resolve = config => config.ctx.dao.getCCEncList
      ),
      Field(
        "ccEncounters",
        OptionType(ListType(CCEncounterType)),
        arguments = List(Ids),
        resolve = config => ccEncounterFetcher.deferSeq(config.arg(Ids))
      ),
      Field(
        "patientMedicalHistoryList",
        OptionType(ListType(PatientMedicalHistoryType)),
        resolve = config => config.ctx.dao.getPatientMedicalHistory
      ),
      Field(
        "patientMedicalHistories",
        OptionType(ListType(PatientMedicalHistoryType)),
        arguments = List(Ids),
        resolve = config => patientMedicalHistoryFetcher.deferSeq(config.arg(Ids))
      ),

      Field(
        "objectiveList",
        OptionType(ListType(ObjectiveDataType)),
        resolve = c => c.ctx.dao.getObjectiveData
      ),
      Field(
        "objectives",
        OptionType(ListType(ObjectiveDataType)),
        arguments = List(Ids),
        resolve = config => objectiveDataFetcher.deferSeq(config.arg(Ids))
      ),

      Field(
        "assessmentList",
        OptionType(ListType(AssessmentDataType)),
        resolve = c => c.ctx.dao.getAssessmentData
      ),
      Field(
        "assessments",
        OptionType(ListType(AssessmentDataType)),
        arguments = List(Ids),
        resolve = config => assessmentDataFetcher.deferSeq(config.arg(Ids))
      ),

      Field(
        "planList",
        OptionType(ListType(PlanDataType)),
        resolve = c => c.ctx.dao.getPlanData
      ),
      Field(
        "plans",
        OptionType(ListType(PlanDataType)),
        arguments = List(Ids),
        resolve = config => planDataFetcher.deferSeq(config.arg(Ids))
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

  /**
   * Provide FromInput for input classes :: To convert from json to case classes
   */
  implicit val AuthProviderEmailInputType: InputObjectType[AuthProviderEmail] = deriveInputObjectType[AuthProviderEmail](
    InputObjectTypeName("AUTH_PROVIDER_EMAIL")
  )

  implicit val authProviderEmailFormat: RootJsonFormat[AuthProviderEmail] = jsonFormat2(AuthProviderEmail)
  implicit val authProviderSignupDataFormat: RootJsonFormat[AuthProviderSignupData] = jsonFormat1(AuthProviderSignupData)

  private lazy val SubjectiveNodeDataInputType: InputObjectType[SubjectiveNodeData] = deriveInputObjectType[SubjectiveNodeData](
    InputObjectTypeName("SUBJECTIVE_NODE_DATA_TYPE")
  )
  implicit val CCEncInputType: InputObjectType[CCEncounter] = deriveInputObjectType[CCEncounter](
    InputObjectTypeName("CC_ENC_INPUT_TYPE")
  )
  implicit val PatientMedicalHistoryInputType: InputObjectType[PatientMedicalHistory] = deriveInputObjectType[PatientMedicalHistory](
    InputObjectTypeName("PATIENT_MEDICAL_HISTORY_INPUT_TYPE")
  )

  implicit val ObjectiveDataInputType: InputObjectType[Objective] = deriveInputObjectType[Objective](
    InputObjectTypeName("OBJECT_INPUT_TYPE")
  )

  implicit val AssessmentDataInputType: InputObjectType[Assessment] = deriveInputObjectType[Assessment](
    InputObjectTypeName("ASSESSMENT_INPUT_TYPE")
  )

  implicit val PlanDataInputType: InputObjectType[Plan] = deriveInputObjectType[Plan](
    InputObjectTypeName("PLAN_INPUT_TYPE")
  )


  implicit val ccEncFormat: RootJsonFormat[CCEncounter] = jsonFormat4(CCEncounter)
  implicit val PatientMedicalHistoryFormat: RootJsonFormat[PatientMedicalHistory] = jsonFormat7(PatientMedicalHistory)
  implicit val ObjectiveFormat: RootJsonFormat[Objective] = jsonFormat6(Objective)
  implicit val AssessmentFormat: RootJsonFormat[Assessment] = jsonFormat4(Assessment)
  implicit val PlanFormat: RootJsonFormat[Plan] = jsonFormat6(Plan)


  implicit val subjectiveDataFormat: RootJsonFormat[SubjectiveNodeData] = jsonFormat4(SubjectiveNodeData)
  private val SubjectiveNodeDataArg = Argument("subjectiveNodeData", SubjectiveNodeDataInputType)
  private val ObjectiveDataArg = Argument("objectNodeData", ObjectiveDataInputType)
  private val AssessmentDataArg = Argument("assessmentNodeData", AssessmentDataInputType)
  private val PlanDataArg = Argument("planNodeData", PlanDataInputType)

  private val PatientIdArg = Argument("patientId", IntType)
  private val SoapPatientIdArg = Argument("soapId", IntType)

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
        SubjectiveNodeDataType,
        arguments = SoapPatientIdArg :: SubjectiveNodeDataArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => {
          val dao = c.ctx.dao
          val subjectiveNodeData: SubjectiveNodeData = c.arg(SubjectiveNodeDataArg)
          for {
            ccEncData <- dao.createCCEnc(subjectiveNodeData.ccEnc)
            patientMedicalHistoryData <- dao.createPatientMedicalHistory(subjectiveNodeData.patientMedicalHistory)
            subjectiveData <- dao.createSubject(subjectiveNodeData)
            subjectiveNodeData <- dao.buildRelationForSubjectNode(c.arg(SoapPatientIdArg), subjectiveData.id, patientMedicalHistoryData.id, ccEncData.id)
          } yield subjectiveNodeData
        }
      ),

      Field("createObject",
        ObjectiveDataType,
        arguments = SoapPatientIdArg :: ObjectiveDataArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.createObject(c.arg(SoapPatientIdArg), c.arg(ObjectiveDataArg))
      ),

      Field("createAssessment",
        AssessmentDataType,
        arguments = SoapPatientIdArg :: AssessmentDataArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.createAssessment(c.arg(SoapPatientIdArg), c.arg(AssessmentDataArg))
      ),

      Field("createPlan",
        PlanDataType,
        arguments = SoapPatientIdArg :: PlanDataArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.createPlan(c.arg(SoapPatientIdArg), c.arg(PlanDataArg))
      ),


      Field("createPatientSOAP",
        PatientSOAPDataType,
        arguments = PatientIdArg :: SubjectiveNodeDataArg :: ObjectiveDataArg :: AssessmentDataArg :: PlanDataArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => {
          val dao = c.ctx.dao
          val subjectiveNodeData: SubjectiveNodeData = c.arg(SubjectiveNodeDataArg)
          val patId = c.arg(PatientIdArg)
          for {
            ccEncData <- dao.createCCEnc(subjectiveNodeData.ccEnc)
            patientMedicalHistoryData <- dao.createPatientMedicalHistory(subjectiveNodeData.patientMedicalHistory)
            patientSoap <- dao.createSoapNode(patId)
            subjectiveData <- dao.createSubject(subjectiveNodeData)
            subjectiveNodeData <- dao.buildRelationForSubjectNode(patientSoap.id, subjectiveData.id, patientMedicalHistoryData.id, ccEncData.id)
            objectiveNodeData <- dao.createObject(patientSoap.id, c.arg(ObjectiveDataArg))
            assessmentNodeData <- dao.createAssessment(patientSoap.id, c.arg(AssessmentDataArg))
            planNodeData <- dao.createPlan(patientSoap.id, c.arg(PlanDataArg))
            buildRelationSoapData <- dao.buildRelationSoap(patId, patientSoap.id, subjectiveData.id, objectiveNodeData.id, assessmentNodeData.id, planNodeData.id)
          } yield PatientSoap(patientSoap.id, patId, subjectiveNodeData, objectiveNodeData, assessmentNodeData, planNodeData, patientSoap.createdAt)
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
