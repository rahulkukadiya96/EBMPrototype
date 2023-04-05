package schema

import config.MyContext
import convertor.ConvertorUtils.transformList
import convertor.Preprocessor.generateAbstract
import generator.PubMedSearch.{buildQueryWithStaticClassifier, executeQuery, totalPages}
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
  private val Query = Argument("query", StringType)
  private val Limit = Argument("limit", IntType)
  private val PageNo = Argument("pageNo", IntType)

  /**
   * conversions between custom data type (LocalDateTime) and type Sangria understand and then back again to custom type.
   */
  implicit val GraphQLDateTime: ScalarType[LocalDateTime] = ScalarType[LocalDateTime](
    "LocalDateTime", // Define the name
    coerceOutput = (localDateTime, _) => DateTimeFormatUtil.fromDateToStr(ofPattern("yyyy-MM-dd HH:mm"), localDateTime).getOrElse(LocalDateTimeCoerceViolation.errorMessage),
    coerceInput = {
      case StringValue(dt, _, _, _, _) => (DateTimeFormatUtil fromStrToDate(ofPattern("yyyy-MM-dd"), dt)).toRight(LocalDateTimeCoerceViolation)
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

  private val PatientType = deriveObjectType[Unit, Patient](
    Interfaces(IdentifiableType),
    AddFields(Field("soaps", ListType(PatientSOAPDataType), resolve = c => patientSoapDataFetcher.deferRelSeq(soapByPatientId, c.value.id))),
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

  private val soapByPatientId = Relation[PatientSoap, Int]("byPatientId", l => Seq(l.patientId))

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
    AddFields(
      Field("Subjective", ListType(SubjectiveNodeDataType), resolve = c => subjectiveDataPmhFetcher.deferRelSeq(subjectByPMHRel, c.value.id))),
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

  private val patientSoapDataFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getSoapData(ids),
    (ctx: MyContext, ids: RelationIds[PatientSoap]) => ctx.dao.getSoapDataByPatientId(ids(soapByPatientId))
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
    ReplaceField("patientId", Field("patient", PatientType, resolve = c => patientListFetcher.defer(c.value.patientId))),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt.get))
  )

  private lazy val PICODataType: ObjectType[Unit, Pico] = deriveObjectType[Unit, Pico](
    Interfaces(IdentifiableType)
  )

  private lazy val ResponseDataType: ObjectType[Unit, Response] = deriveObjectType[Unit, Response]()

  implicit val FetchPicoRequestFormat: RootJsonFormat[FetchPicoRequest] = jsonFormat3(FetchPicoRequest)
  implicit val FetchPicoRequestInputType: InputObjectType[FetchPicoRequest] = deriveInputObjectType[FetchPicoRequest](
    InputObjectTypeName("FETCH_PICO_REQUEST_INPUT_TYPE")
  )

  private val FetchPicoRequestArg = Argument("data", FetchPicoRequestInputType)

  implicit val ArticleDataType: ObjectType[Unit, Article] = deriveObjectType[Unit, Article]()
  private lazy val ArticleListResponseType: ObjectType[Unit, ArticleListResponse] = deriveObjectType[Unit, ArticleListResponse]()


  private lazy val BaseResponseDataType: ObjectType[Unit, BaseResponse] = deriveObjectType[Unit, BaseResponse]()

  val resolver: DeferredResolver[MyContext] =
    DeferredResolver.fetchers(
      patientListFetcher,
      ccEncounterFetcher,
      patientMedicalHistoryFetcher,
      subjectiveDataEncFetcher,
      subjectiveDataPmhFetcher,
      objectiveDataFetcher,
      assessmentDataFetcher,
      planDataFetcher,
      patientSoapDataFetcher
    )

  private val queryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field(
        "patientList",
        ListType(PatientType),
        resolve = c => c.ctx.dao.patientList
      ),

      Field(
        "patient",
        OptionType(PatientType),
        arguments = Id :: Nil,
        //resolve = config => config.ctx.dao.getPatient(config.arg(Id))
        resolve = config => patientListFetcher.deferOpt(config.arg(Id))
      ),

      Field(
        "patients",
        OptionType(ListType(PatientType)),
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
      ),

      Field(
        "soapList",
        OptionType(ListType(PatientSOAPDataType)),
        resolve = c => c.ctx.dao.getSoapData
      ),
      Field(
        "soap",
        OptionType(ListType(PatientSOAPDataType)),
        arguments = List(Ids),
        resolve = config => patientSoapDataFetcher.deferSeq(config.arg(Ids))
      ),
      Field(
        "pico",
        OptionType(ListType(PICODataType)),
        arguments = FetchPicoRequestArg :: Nil,
        resolve = config => {
          val dao = config.ctx.dao
          val fetchPicoRequest = config.arg(FetchPicoRequestArg)
          for {
            patientSoapList <- dao.getSoapData(fetchPicoRequest.ids)
          } yield transformList(fetchPicoRequest.comparison)(patientSoapList)
        }
      ),
      Field(
        "fetch_pico",
        OptionType(ListType(PICODataType)),
        arguments = Id :: Nil,
        resolve = config => {
          for {
            picoData <- config.ctx.dao.getPicoDataBySoapId(config.arg(Id))
          } yield picoData
        }
      ),
      Field(
        "build_query",
        OptionType(ResponseDataType),
        arguments = Id :: Nil,
        resolve = config => {
          val dao = config.ctx.dao
          val meSHLoaderDao = config.ctx.meSHLoader
          for {
            pico <- dao.getPicoDataBySoapId(config.arg(Id))
            query <- buildQueryWithStaticClassifier(pico.headOption, meSHLoaderDao, dao)
          } yield Response(Option.empty, 200, Option("Success"), query)
        }
      ),
      Field(
        "fetch_article",
        OptionType(ArticleListResponseType),
        arguments = Id :: Limit :: PageNo :: Nil,
        resolve = config => {
          val dao = config.ctx.dao
          val limit = config.arg(Limit)

          for {
            pico <- dao.getPicoDataBySoapId(config.arg(Id))
            cnt <- dao.fetchArticleCount(pico.headOption.get.id)
            totalPages <- totalPages(cnt, limit)
            articles <- dao.fetchArticles(pico.headOption.get.id, config.arg(PageNo), limit)
          } yield ArticleListResponse(200, Option("Success"), Option(totalPages), Option(articles))
        }
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

  implicit val PicoDataInputType : InputObjectType[Pico] = deriveInputObjectType[Pico](
    InputObjectTypeName("PICO_INPUT_TYPE")
  )


  implicit val ccEncFormat: RootJsonFormat[CCEncounter] = jsonFormat4(CCEncounter)
  implicit val PatientMedicalHistoryFormat: RootJsonFormat[PatientMedicalHistory] = jsonFormat7(PatientMedicalHistory)
  implicit val ObjectiveFormat: RootJsonFormat[Objective] = jsonFormat6(Objective)
  implicit val AssessmentFormat: RootJsonFormat[Assessment] = jsonFormat4(Assessment)
  implicit val PlanFormat: RootJsonFormat[Plan] = jsonFormat6(Plan)
  implicit val PicoFormat: RootJsonFormat[Pico] = jsonFormat8(Pico)


  implicit val subjectiveDataFormat: RootJsonFormat[SubjectiveNodeData] = jsonFormat4(SubjectiveNodeData)
  private val SubjectiveNodeDataArg = Argument("subjectiveNodeData", SubjectiveNodeDataInputType)
  private val ObjectiveDataArg = Argument("objectNodeData", ObjectiveDataInputType)
  private val AssessmentDataArg = Argument("assessmentNodeData", AssessmentDataInputType)
  private val PlanDataArg = Argument("planNodeData", PlanDataInputType)
  private val PicoDataArg = Argument("pico", PicoDataInputType)

  private val PatientIdArg = Argument("patientId", IntType)
  private val SoapPatientIdArg = Argument("soapId", IntType)
  private val PicoIdArg = Argument("picoId", IntType)
  private val MeshPathArg = Argument("filePath", StringType)

  private val Mutation = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field("createPatient",
        PatientType,
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
            _ <- dao.buildRelationSoap(patId, patientSoap.id, subjectiveData.id, objectiveNodeData.id, assessmentNodeData.id, planNodeData.id)
          } yield PatientSoap(patientSoap.id, patId, subjectiveNodeData, objectiveNodeData, assessmentNodeData, planNodeData, patientSoap.createdAt)
        }
      ),

      Field("deletePatientSOAP",
        BooleanType,
        arguments = SoapPatientIdArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.deleteSoap(c.arg(SoapPatientIdArg))
      ),

      Field("loadMeshTerms",
        BooleanType,
        arguments = MeshPathArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.meSHLoader.loadDictionary(c.arg(MeshPathArg))
      ),

      Field("create_pico",
        PICODataType,
        arguments = SoapPatientIdArg :: PicoDataArg :: Nil,
        resolve = c => {
          val dao = c.ctx.dao
          val pico = c.arg(PicoDataArg)
          val soapId = c.arg(SoapPatientIdArg)
          for {
            pico <- dao.createPico(pico)
            isRelationCreated <- dao.buildRelationSoapPico(soapId, pico.id)
          } yield pico
        }
      ),

      Field("update_pico",
        PICODataType,
        arguments = PicoDataArg :: Nil,
        resolve = c => {
          val dao = c.ctx.dao
          val pico = c.arg(PicoDataArg)
          for {
            pico <- dao.updatePico(pico)
          } yield pico
        }
      ),

      Field("update_query",
        PICODataType,
        arguments = SoapPatientIdArg :: Query :: Nil,
        resolve = c => {
          val dao = c.ctx.dao
          val soapId = c.arg(SoapPatientIdArg)
          val searchQuery = c.arg(Query)
          for {
            picoData <- dao.getPicoDataBySoapId(soapId)
            pico <- dao.updateQuery(picoData.headOption.get.id, searchQuery)
          } yield {
            dao.removeAllArticles(picoData.headOption.get.id)
            pico
          }
        }
      ),
      Field(
        "execute_query",
        OptionType(ResponseDataType),
        arguments = Query :: SoapPatientIdArg :: Limit :: Nil,
        resolve = config => {
          val dao = config.ctx.dao
          val soapId = config.arg(SoapPatientIdArg)
          for {
            pico <- dao.getPicoDataBySoapId(soapId)
            data <- executeQuery(pico.headOption, Option(config.arg(Query)), dao, config.arg(Limit))
          } yield data
        }
      ),
      Field(
        "generate_abstract",
        OptionType(BaseResponseDataType),
        arguments = SoapPatientIdArg :: Nil,
        resolve = config => {
          val dao = config.ctx.dao
          val soapId = config.arg(SoapPatientIdArg)
          for {
            pico <- dao.getPicoDataBySoapId(soapId)
            articles <- dao.fetchAllArticles(pico.headOption.get.id)
            articleSummary <- generateAbstract(articles)
            isSaved <- dao.saveArticleSummary(articleSummary)
          } yield BaseResponse(statusCode = 200, message = Some("Success"))
        }
      )

      /*Field("updatePatientSOAP",
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
            _ <- dao.buildRelationSoap(patId, patientSoap.id, subjectiveData.id, objectiveNodeData.id, assessmentNodeData.id, planNodeData.id)
          } yield PatientSoap(patientSoap.id, patId, subjectiveNodeData, objectiveNodeData, assessmentNodeData, planNodeData, patientSoap.createdAt)
        }
      ),*/

      /*Field("Login",
        PatientType,
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
