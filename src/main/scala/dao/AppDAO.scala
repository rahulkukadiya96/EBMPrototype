package dao

import models._
import org.neo4j.driver.v1.{Driver, Record}
import utility.DateTimeFormatUtil.getCurrentUTCTime

import java.lang.Math.{max, min}
import java.time.LocalDateTime
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters

class AppDAO(connection: Driver) {
  def createPatient(patient: Patient): Future[Patient] = {
    val queryString = s"CREATE (patient : Patient{name : '${patient.name}', age : ${patient.age}, address : '${patient.address}', createAt : ${getTodayDateTimeNeo4j(patient.createdAt)} }) RETURN ID(patient) as id, patient.name as name, patient.age as age, patient.address as address, patient.createAt as createAt"
    writeData(queryString, readPatient)
  }


  def createCCEnc(ccEnc: CCEncounter): Future[CCEncounter] = {
    val queryString = s"CREATE (n : CCEncounter{signs : '${ccEnc.signs}', symptoms : '${ccEnc.symptoms}', createdAt : ${getTodayDateTimeNeo4j(ccEnc.createdAt.getOrElse(getCurrentUTCTime))} }) RETURN ID(n) as ccEncId, n.signs as signs, n.symptoms as symptoms, n.createdAt as ccEncCreatedAt"
    writeData(queryString, readCCEncounter)
  }


  def createSubject(subjective: SubjectiveNodeData): Future[Subjective] = {
    val queryString = s"CREATE (subjective : Subjective{ createdAt : ${getTodayDateTimeNeo4j(subjective.createdAt.getOrElse(getCurrentUTCTime))} }) RETURN ID(subjective) as id, subjective.createdAt as createdAt"
    writeData(queryString, readSubjective)
  }


  def createPatientMedicalHistory(patientMedicalHistory: PatientMedicalHistory): Future[PatientMedicalHistory] = {
    val queryString = s"CREATE (n : PatientMedicalHistory{medications : '${patientMedicalHistory.medications}', allergies : '${patientMedicalHistory.allergies}', procedure : '${patientMedicalHistory.procedure}', familyHistory : '${patientMedicalHistory.familyHistory}', demographics : '${patientMedicalHistory.demographics}',createdAt : ${getTodayDateTimeNeo4j(patientMedicalHistory.createdAt.getOrElse(getCurrentUTCTime))} }) RETURN ID(n) as patientMedicalHistoryId, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory as familyHistory, n.demographics as demographics, n.createdAt as patientMedicalHistoryCreatedAt"
    writeData(queryString, readPatientMedicalHistory)
  }

  def buildRelationForSubjectNode(patientSoapId: Int, subId: Int, patMedId: Int, ccEncId: Int): Future[SubjectiveNodeData] = {
    var queryString = s"MATCH (patientSOAP: Patient_SOAP) WHERE ID(patientSOAP) = $patientSoapId "
    queryString += s"MATCH (subjective: Subjective) WHERE ID(subjective) = $subId "
    queryString += s"MATCH (patientMedicalHistory: PatientMedicalHistory ) WHERE ID(patientMedicalHistory) = $patMedId "
    queryString += s"MATCH (ccEnc: CCEncounter ) WHERE ID(ccEnc) = $ccEncId "


    /*queryString += s" CREATE (patientSOAP)-[:soap_subject { soap_subject_id : $subId  }]->(subjective), "
    queryString += s" (subjective)-[:patientSOAP {patientSOAPId : $patientSoapId }]->(patientSOAP), "*/

    queryString += s" CREATE (subjective)-[:pmh { patMedId : $patMedId }]->(patientMedicalHistory),"
    queryString += s" (patientMedicalHistory)-[:subId { subId : $subId }]->(subjective), "

    queryString += s" (subjective)-[:ccEnc { ccEnc : $ccEncId }]->(ccEnc), "
    queryString += s" (ccEnc)-[:subId { subId : $subId }]->(subjective) "

    queryString += s" RETURN ID(subjective) as subjectiveId, subjective.createdAt as createdAt, "
    queryString += s" ID(patientMedicalHistory) as patientMedicalHistoryId, patientMedicalHistory.medications as medications, patientMedicalHistory.allergies as allergies, patientMedicalHistory.procedure as procedure, patientMedicalHistory.familyHistory as familyHistory, patientMedicalHistory.demographics as demographics, patientMedicalHistory.createdAt as patientMedicalHistoryCreatedAt, "
    queryString += s" ID(ccEnc) as ccEncId, ccEnc.signs as signs, ccEnc.symptoms as symptoms, ccEnc.createdAt as ccEncCreatedAt"

    writeData(queryString, readSubjectiveNodeData)
  }


  private val returnObjGenQuery = s" ID(objective) as objectiveId, objective.vital as vital, objective.labTest as labTest, objective.physicalExam as physicalExam, objective.diagnosticData as diagnosticData, objective.createdAt as objectiveCreatedAt"

  def createObject(patientSoapId: Int, objective: Objective): Future[Objective] = {
    var queryString = s"MATCH (patientSOAP: Patient_SOAP) WHERE ID(patientSOAP) = $patientSoapId "
    queryString = s"CREATE (objective : Objective{ vital: '${objective.vital}', labTest: '${objective.labTest}', physicalExam: '${objective.physicalExam}', diagnosticData: '${objective.diagnosticData}', createdAt : ${getTodayDateTimeNeo4j(objective.createdAt.getOrElse(getCurrentUTCTime))} }) RETURN"

    /*queryString += s" CREATE (patientSOAP)-[:soap_object { subId :ID(objective)  }]->(objective), "
    queryString += s" (objective)-[:patientSOAP {patientSOAPId : $patientSoapId }]->(patientSOAP) "*/

    queryString += returnObjGenQuery
    writeData(queryString, readObjective)
  }

  private val returnAssessmentGenQuery = s" ID(assessment) as assessmentId, assessment.ddx as ddx, assessment.mechanism as mechanism, assessment.createdAt as assessmentCreatedAt"

  def createAssessment(patientSoapId: Int, assessment: Assessment): Future[Assessment] = {
    var queryString = s"MATCH (patientSOAP: Patient_SOAP) WHERE ID(patientSOAP) = $patientSoapId "
    queryString = s"CREATE (assessment : Assessment{ ddx: '${assessment.ddx}', mechanism: '${assessment.mechanism}', createdAt : ${getTodayDateTimeNeo4j(assessment.createdAt.getOrElse(getCurrentUTCTime))} }) RETURN "

    /*queryString += s" CREATE (patientSOAP)-[:soap_assessment { subId :ID(assessment)  }]->(assessment), "
    queryString += s" (assessment)-[:patientSOAP {patientSOAPId : $patientSoapId }]->(patientSOAP) "*/

    queryString += returnAssessmentGenQuery
    writeData(queryString, readAssessment)
  }

  private val returnPlanGenQuery = s" ID(plan) as planId, plan.indication as indication, plan.management as management, plan.summary as summary, plan.followup as followup, plan.createdAt as planCreatedAt"

  def createPlan(patientSoapId: Int, plan: Plan): Future[Plan] = {
    var queryString = s"CREATE (plan : Plan{ indication: '${plan.indication}', management: '${plan.management}',  summary: '${plan.summary}',  followup: '${plan.followup}', createdAt : ${getTodayDateTimeNeo4j(plan.createdAt.getOrElse(getCurrentUTCTime))} }) RETURN"
    queryString += returnPlanGenQuery
    writeData(queryString, readPlan)
  }

  def createSoapNode(patientId: Int): Future[PatientSoap] = {
    var queryString = s"CREATE (soap : Patient_SOAP{ patientId: $patientId,  createdAt : ${getTodayDateTimeNeo4j(getCurrentUTCTime)} }) "

    queryString += "RETURN ID(soap) as soapId, soap.patientId as patientId, soap.createdAt as soapCreatedAt"
    writeData(queryString, readPatientSoapCreatedNode)
  }

  def buildRelationSoap(patId: Int, soapNodeId: Int, subId: Int, objectNodeId: Int, assessmentNodeId: Int, planNodeId: Int): Future[PatientSoap] = {
    var queryString = s"MATCH (patientSOAP: Patient_SOAP) WHERE ID(patientSOAP) = $soapNodeId "
    queryString += s"MATCH (patient: Patient) WHERE ID(patient) = $patId "
    queryString += s"MATCH (subjective: Subjective) WHERE ID(subjective) = $subId "
    queryString += s"MATCH (objective: Objective) WHERE ID(objective) = $objectNodeId "
    queryString += s"MATCH (assessment: Assessment) WHERE ID(assessment) = $assessmentNodeId "
    queryString += s"MATCH (plan: Plan) WHERE ID(plan) = $planNodeId "

    queryString += s" CREATE (patientSOAP)-[:soap_subject { soap_subject_id : $subId  }]->(subjective), "
    queryString += s" (subjective)-[:subject_soap {patientSOAPId : $soapNodeId }]->(patientSOAP) "

    queryString += s" CREATE (patientSOAP)-[:soap_object { soap_object_id :$objectNodeId  }]->(objective), "
    queryString += s" (objective)-[:object_soap {patientSOAPId : $soapNodeId }]->(patientSOAP)"

    queryString += s" CREATE (patientSOAP)-[:soap_assessment { soap_assessment_id :$assessmentNodeId  }]->(assessment), "
    queryString += s" (assessment)-[:assessment_soap {patientSOAPId : $soapNodeId }]->(patientSOAP) "

    queryString += s" CREATE (patientSOAP)-[:soap_plan { soap_plan_Id :$planNodeId }]->(plan), "
    queryString += s" (plan)-[:pan_soap {patId : $soapNodeId }]->(patientSOAP) "

    queryString += s" CREATE (patient)-[:patient_soap { patientSOAPId :ID(patientSOAP)  }]->(patientSOAP), "
    queryString += s" (patientSOAP)-[:soap_patient {patId : $patId }]->(patient) "

    queryString += "RETURN ID(patientSOAP) as soapId, patientSOAP.patientId as patientId, patientSOAP.createdAt as soapCreatedAt"

    writeData(queryString, readPatientSoapCreatedNode)
  }

  private val returnPicoGenQuery = s" ID(pico) as picoId, pico.problem as problem, pico.intervention as intervention, pico.comparison as comparison, pico.outcome as outcome,pico.timePeriod as timePeriod, pico.createdAt as picoCreatedAt, pico.searchQuery as searchQuery"

  def createPico(pico: Pico): Future[Pico] = {
    var queryString = s"CREATE (pico : Pico{ problem: '${pico.problem}', intervention: '${pico.intervention}', comparison: '${pico.comparison.getOrElse("")}', outcome: '${pico.outcome}', timePeriod: '${pico.timePeriod.getOrElse("")}', createdAt : ${getTodayDateTimeNeo4j(pico.createdAt.getOrElse(getCurrentUTCTime))} }) RETURN"
    queryString += returnPicoGenQuery
    writeData(queryString, readPico)
  }

  def buildRelationSoapPico(soapNodeId: Int, picoId: Int): Future[Boolean] = Future {
    var queryString = s"MATCH (patientSOAP: Patient_SOAP) WHERE ID(patientSOAP) = $soapNodeId "
    queryString += s"MATCH (pico: Pico) WHERE ID(pico) = $picoId "

    queryString += s" CREATE (patientSOAP)-[:soap_pico { picoId :$picoId}]->(pico), "
    queryString += s" (pico)-[:pico_soap {patientSOAPId : $soapNodeId }]->(patientSOAP) "
    connection.session().run(queryString)
    true
  }

  def updatePico(pico: Pico): Future[Pico] = {
    var queryString = s"MATCH (pico:Pico) WHERE ID(pico) = ${pico.id}  SET pico.problem = '${pico.problem}', pico.intervention = '${pico.intervention}', pico.comparison = '${pico.comparison.getOrElse("")}', pico.timePeriod = '${pico.timePeriod.getOrElse("")}', pico.outcome = '${pico.outcome}' REMOVE pico.searchQuery RETURN "
    queryString += returnPicoGenQuery
    writeData(queryString, readPico)
  }

  def updateQuery(picoId: Int, query : String): Future[Pico] = {
    var queryString = s"MATCH (pico:Pico) WHERE ID(pico) = $picoId  SET pico.searchQuery = '$query' RETURN "
    queryString += returnPicoGenQuery
    writeData(queryString, readPico)
  }

  def saveArticles(picoId: Int, articles: Seq[Article]): Unit = {
    val session = connection.session()
    try {
      val createArticleQuery = s"MATCH (pico:Pico) WHERE id(pico) = $picoId" +
        " CREATE (pico) -[:HAS_ARTICLE {picoId: id(pico)}]-> (a:Article {title: $title, authors: $authors, journal:$journal, pubDate: $pubDate, abstractText: $abstractText, summary: $summary, background: $background, objectives: $objectives, methods: $methods, result: $result, conclusion: $conclusion}) RETURN id(a) AS articleId"
      articles.map(article => {
        val background = article.abstractComponent.map(_.background.getOrElse("")).getOrElse("")
        val objectives = article.abstractComponent.map(_.objectives.getOrElse("")).getOrElse("")
        val methods = article.abstractComponent.map(_.methods.getOrElse("")).getOrElse("")
        val result = article.abstractComponent.map(_.result.getOrElse("")).getOrElse("")
        val conclusion = article.abstractComponent.map(_.conclusion.getOrElse("")).getOrElse("")

        val params: Map[String, Object] = Map(
          "title" -> article.title,
          "authors" -> article.authors,
          "journal" -> article.journal,
          "pubDate" -> article.pubDate,
          "abstractText" -> article.abstractText,

          "summary" -> article.summary.getOrElse(""),

          "background" -> background,
          "objectives" -> objectives,
          "methods" -> methods,
          "result" -> result,
          "conclusion" -> conclusion,
        )
        print(s"Params is $params")
        session.run(createArticleQuery, params.asJava)
      })
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      session.close()
    }
  }

  def saveArticleSummary(summaries: Map[Long, String]): Future[Boolean] = Future {
    val query = "MATCH (a:Article) WHERE ID(a) = $articleId SET a.summary = $summary RETURN ID(a) AS articleId"
    val session = connection.session()
    try {
      /*summaries.map { case (articleId, summary) =>
        val params: Map[String, Object] = Map("articleId" -> articleId, "summary" -> summary)
        session.run(query, params.asJava)
      }*/
      summaries.foreach(entry => {
        val params: Map[String, AnyRef] = Map("articleId" -> entry._1.asInstanceOf[AnyRef], "summary" -> entry._2)
        session.run(query, params.asJava)
      })
      true
    } catch {
      case e: Exception =>
        e.printStackTrace()
        false
    } finally {
      session.close()
    }
  }

  private val RETURN_ARTICLE = " id(a) AS articleId, a.title as title, a.authors as authors, a.journal as journal, a.pubDate as pubDate , a.abstractText as abstractText, a.summary as summary, a.background as background, a.objectives as objectives, a.methods as methods, a.result as result, a.conclusion as conclusion  "
  def fetchArticles(picoId: Int, pageNo: Int, limit : Int): Future[Seq[Article]] = {
    val skip = max((pageNo - 1) * limit, 0)
    val ORDER_PAGINATION = s" ORDER BY a.title SKIP $skip LIMIT $limit"
    val queryString = s" MATCH (n:Pico) - [:HAS_ARTICLE] -> (a:Article) WHERE ID(n) = $picoId   RETURN " + RETURN_ARTICLE + ORDER_PAGINATION
    getData(queryString, readArticle)
  }

  def fetchAllArticles(picoId: Int): Future[Seq[Article]] = {
    val queryString = s" MATCH (n:Pico) - [:HAS_ARTICLE] -> (a:Article) WHERE ID(n) = $picoId   RETURN " + RETURN_ARTICLE
    getData(queryString, readArticle)
  }

  def fetchArticleCount(picoId: Int): Future[Int] = {
    val queryString = s" MATCH (n:Pico) - [:HAS_ARTICLE] -> (a:Article) WHERE ID(n) = $picoId RETURN count(id(a)) as cnt"
    writeData(queryString, readCnt)
  }

  def removeAllArticles(picoId: Int): Unit = {
    val query = s"MATCH (p:Pico)-[r:HAS_ARTICLE]->(a:Article) WHERE r.picoId = $picoId DETACH DELETE a "
    val session = connection.session()
    try {
      session.run(query)
    } finally {
      session.close()
    }
  }

  /* Patient methods */

  def patientList: Future[Seq[Patient]] = {
    val queryString = "MATCH (n: Patient) RETURN ID(n) as id, n.name as name, n.age as age, n.address as address, n.createAt as createAt"
    getData(queryString, readPatient)
  }

  def getPatients(ids: Seq[Int]): Future[Seq[Patient]] = {
    val queryString = s"MATCH (n: Patient) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as id, n.name as name, n.age as age, n.address as address, n.createAt as createAt"
    getData(queryString, readPatient)
  }

  /* Subject Node methods */

  def getSubjectiveList: Future[Seq[SubjectiveNodeData]] = {
    var queryString = s"MATCH (subjective:Subjective) -[:pmh]-> (patientMedicalHistory:PatientMedicalHistory), (subjective) -[:ccEnc] -> (ccEnc:CCEncounter) "
    queryString += s" RETURN ID(subjective) as subjectiveId, subjective.createdAt as createdAt, "
    queryString += s" ID(patientMedicalHistory) as patientMedicalHistoryId, patientMedicalHistory.medications as medications, patientMedicalHistory.allergies as allergies, patientMedicalHistory.procedure as procedure, patientMedicalHistory.familyHistory as familyHistory, patientMedicalHistory.demographics as demographics, patientMedicalHistory.createdAt as patientMedicalHistoryCreatedAt, "
    queryString += s" ID(ccEnc) as ccEncId, ccEnc.signs as signs, ccEnc.symptoms as symptoms, ccEnc.createdAt as ccEncCreatedAt"
    getData(queryString, readSubjectiveNodeData)
  }

  private val returnSubjectiveGenQuery = " ID(subjective) as subjectiveId, subjective.createdAt as createdAt, ID(patientMedicalHistory) as patientMedicalHistoryId, patientMedicalHistory.medications as medications, patientMedicalHistory.allergies as allergies, patientMedicalHistory.procedure as procedure, patientMedicalHistory.familyHistory as familyHistory, patientMedicalHistory.demographics as demographics, patientMedicalHistory.createdAt as patientMedicalHistoryCreatedAt,  ID(ccEnc) as ccEncId, ccEnc.signs as signs, ccEnc.symptoms as symptoms, ccEnc.createdAt as ccEncCreatedAt"

  def getSubjectiveData(ids: Seq[Int]): Future[Seq[SubjectiveNodeData]] = {
    var queryString = s"MATCH (subjective:Subjective) -[:pmh]-> (patientMedicalHistory:PatientMedicalHistory), (subjective) -[:ccEnc] -> (ccEnc:CCEncounter) "
    queryString += s" WHERE ID(subjective) IN [${ids.mkString(",")}] RETURN "
    queryString += returnSubjectiveGenQuery
    getData(queryString, readSubjectiveNodeData)
  }

  /* CCEnc methods */

  def getCCEncList: Future[Seq[CCEncounter]] = {
    val queryString = s"MATCH (ccEncounter: CCEncounter)  RETURN ID(ccEncounter) as ccEncId, ccEncounter.signs as signs, ccEncounter.symptoms as symptoms, ccEncounter.createdAt as ccEncCreatedAt"
    getData(queryString, readCCEncounter)
  }

  def getCCEncounter(ids: Seq[Int]): Future[Seq[CCEncounter]] = {
    val queryString = s"MATCH (n: CCEncounter) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as ccEncId, n.signs as signs, n.symptoms as symptoms, n.createdAt as ccEncCreatedAt"
    getData(queryString, readCCEncounter)
  }

  def getSubjectiveNodeDataByEncId(ids: Seq[Int]): Future[Seq[Int]] = {
    val queryString = s"MATCH (subjective :Subjective)-[r:ccEnc]->(ccEnc:CCEncounter) WHERE r.ccEnc = ${ids.mkString(",")} RETURN ID(subjective) as subjectiveId"
    getData(queryString, readSubjectiveNodeId)
  }

  /* Patient Medical History methods */
  def getPatientMedicalHistory(ids: Seq[Int]): Future[Seq[PatientMedicalHistory]] = {
    val queryString = s"MATCH (n: PatientMedicalHistory) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as patientMedicalHistoryId, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory as familyHistory, n.demographics as demographics, n.createdAt as patientMedicalHistoryCreatedAt"
    getData(queryString, readPatientMedicalHistory)
  }

  def getPatientMedicalHistory: Future[Seq[PatientMedicalHistory]] = {
    val queryString = s"MATCH (n: PatientMedicalHistory) RETURN ID(n) as patientMedicalHistoryId, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory as familyHistory, n.demographics as demographics, n.createdAt as patientMedicalHistoryCreatedAt"
    getData(queryString, readPatientMedicalHistory)
  }

  def getSubjectiveNodeDataByPmhId(ids: Seq[Int]): Future[Seq[Int]] = {
    val queryString = s"MATCH (subjective :Subjective)-[r:pmh]->(:PatientMedicalHistory) WHERE r.patMedId = ${ids.mkString(",")} RETURN ID(subjective) as subjectiveId"
    getData(queryString, readSubjectiveNodeId)
  }

  def getObjectiveData: Future[Seq[Objective]] = {
    val queryString = s"MATCH (objective:Objective) RETURN " + returnObjGenQuery
    getData(queryString, readObjective)
  }

  def getObjectiveData(ids: Seq[Int]): Future[Seq[Objective]] = {
    val queryString = s"MATCH (objective:Objective) WHERE ID(objective) IN [${ids.mkString(",")} ] RETURN " + returnObjGenQuery
    getData(queryString, readObjective)
  }


  def getAssessmentData: Future[Seq[Assessment]] = {
    val queryString = s"MATCH (assessment:Assessment) RETURN " + returnAssessmentGenQuery
    getData(queryString, readAssessment)
  }

  def getAssessmentData(ids: Seq[Int]): Future[Seq[Assessment]] = {
    val queryString = s"MATCH (assessment:Assessment) WHERE ID(assessment) IN [${ids.mkString(",")} ] RETURN " + returnAssessmentGenQuery
    getData(queryString, readAssessment)
  }

  def getPlanData: Future[Seq[Plan]] = {
    val queryString = s"MATCH (plan:Plan) RETURN " + returnPlanGenQuery
    getData(queryString, readPlan)
  }

  def getPlanData(ids: Seq[Int]): Future[Seq[Plan]] = {
    val queryString = s"MATCH (plan:Plan) WHERE ID(plan) IN [${ids.mkString(",")} ] RETURN " + returnPlanGenQuery
    getData(queryString, readPlan)
  }

  private val returnGenStForPatientSoap = "ID(patientSOAP) as soapId, patientSOAP.patientId as patientId, patientSOAP.createdAt as soapCreatedAt"

  def getSoapData: Future[Seq[PatientSoap]] = {
    var queryString = s"MATCH (patientSOAP:Patient_SOAP) - [:soap_subject] -> (subjective:Subjective), (patientSOAP) - [:soap_object] -> (objective:Objective), (patientSOAP) - [:soap_assessment] -> (assessment:Assessment), (patientSOAP) - [:soap_plan] -> (plan:Plan), "
    queryString += " (subjective) -[:pmh]-> (patientMedicalHistory:PatientMedicalHistory), (subjective) -[:ccEnc] -> (ccEnc:CCEncounter) RETURN "
    queryString += returnGenStForPatientSoap + ", "
    queryString += returnSubjectiveGenQuery + ", "
    queryString += returnObjGenQuery + ", "
    queryString += returnAssessmentGenQuery + ","
    queryString += returnPlanGenQuery
    getData(queryString, readPatientSoapNode)
  }

  def getSoapData(ids: Seq[Int]): Future[Seq[PatientSoap]] = {
    var queryString = s"MATCH (patientSOAP:Patient_SOAP) - [:soap_subject] -> (subjective:Subjective), (patientSOAP) - [:soap_object] -> (objective:Objective), (patientSOAP) - [:soap_assessment] -> (assessment:Assessment), (patientSOAP) - [:soap_plan] -> (plan:Plan), "
    queryString += s"(subjective) -[:pmh]-> (patientMedicalHistory:PatientMedicalHistory), (subjective) -[:ccEnc] -> (ccEnc:CCEncounter)  WHERE ID(patientSOAP) IN [${ids.mkString(",")} ] RETURN  "
    queryString += returnGenStForPatientSoap + ", "
    queryString += returnSubjectiveGenQuery + ", "
    queryString += returnObjGenQuery + ", "
    queryString += returnAssessmentGenQuery + ","
    queryString += returnPlanGenQuery
    getData(queryString, readPatientSoapNode)
  }

  def getSoapDataByPatientId(ids: Seq[Int]): Future[Seq[PatientSoap]] = {
    var queryString = s"MATCH (patientSOAP:Patient_SOAP) - [:soap_subject] -> (subjective:Subjective), (patientSOAP) - [:soap_object] -> (objective:Objective), (patientSOAP) - [:soap_assessment] -> (assessment:Assessment), (patientSOAP) - [:soap_plan] -> (plan:Plan), "
    queryString += s"(subjective) -[:pmh]-> (patientMedicalHistory:PatientMedicalHistory), (subjective) -[:ccEnc] -> (ccEnc:CCEncounter)  WHERE patientSOAP.patientId IN [${ids.mkString(",")} ] RETURN  "
    queryString += returnGenStForPatientSoap + ", "
    queryString += returnSubjectiveGenQuery + ", "
    queryString += returnObjGenQuery + ", "
    queryString += returnAssessmentGenQuery + ","
    queryString += returnPlanGenQuery
    getData(queryString, readPatientSoapNode)
  }

  def getPicoDataBySoapId(soapId: Int): Future[Seq[Pico]] = {
    val queryString = s"MATCH (pico : Pico)-[k:pico_soap]->(patientSOAP:Patient_SOAP) WHERE k.patientSOAPId =$soapId RETURN " + returnPicoGenQuery
    getData(queryString, readPico)
  }


  def deleteSoap(id: Int): Future[Boolean] = {
    val queryString = s"MATCH (patientSOAP:Patient_SOAP) WHERE ID(patientSOAP) = $id DETACH DELETE patientSOAP"
    Future {
      deleteNode(queryString)
    }
  }

  private def writeData[T](query: String, reader: Record => T) = {
    val session = connection.session()
    val queryCompletion = session
      .runAsync(query)
      .thenCompose[java.util.List[T]](c => c.listAsync[T](r => reader(r)))
      .thenApply[T] {
        _.asScala.head
      }
      .whenComplete((_, _) => session.closeAsync())

    FutureConverters.CompletionStageOps(queryCompletion).asScala
  }

  private def getData[T](query: String, reader: Record => T) = {
    val session = connection.session()
    val queryCompletion = session
      .runAsync(query)
      .thenCompose[java.util.List[T]](c => c.listAsync[T](record => reader(record)))
      .thenApply[Seq[T]] {
        _.asScala.toSeq
      }
      .whenComplete((_, _) => session.closeAsync())

    FutureConverters.CompletionStageOps(queryCompletion).asScala
  }

  private def deleteNode(query: String) = connection.session().run(query).summary().counters().nodesDeleted() > 0

  private def getTodayDateTimeNeo4j(localDateTime: LocalDateTime): String = {
    s"""localdatetime(
       |{date:date({ year:${localDateTime.getYear}, month:${localDateTime.getMonthValue}, day:${localDateTime.getDayOfMonth}}),
       | time: localtime({ hour:${localDateTime.getHour}, minute:${localDateTime.getMinute}, second:${localDateTime.getSecond}})})
       | """.stripMargin
  }


  private def readPatient(record: Record): Patient = {
    Patient(
      id = record.get("id").asInt(),
      name = record.get("name").asString(),
      age = record.get("age").asInt(),
      address = record.get("address").asString(),
      createdAt = record.get("createAt").asLocalDateTime()
    )
  }

  private def readCCEncounter(record: Record): CCEncounter = {
    CCEncounter(
      id = record.get("ccEncId").asInt(),
      signs = record.get("signs").asString(),
      symptoms = record.get("symptoms").asString(),
      createdAt = Some(record.get("ccEncCreatedAt").asLocalDateTime())
    )
  }

  private def readPatientMedicalHistory(record: Record): PatientMedicalHistory = {
    PatientMedicalHistory(
      id = record.get("patientMedicalHistoryId").asInt(),
      medications = record.get("medications").asString(),
      allergies = record.get("allergies").asString(),
      procedure = record.get("procedure").asString(),
      familyHistory = record.get("familyHistory").asString(),
      demographics = record.get("demographics").asString(),
      createdAt = Some(record.get("patientMedicalHistoryCreatedAt").asLocalDateTime())
    )
  }

  private def readSubjective(record: Record): Subjective = {
    Subjective(
      id = record.get("id").asInt(),
      createdAt = Some(record.get("createdAt").asLocalDateTime())
    )
  }

  private def readSubjectiveNodeData(record: Record): SubjectiveNodeData = {
    SubjectiveNodeData(
      id = record.get("subjectiveId").asInt(),
      createdAt = Some(record.get("createdAt").asLocalDateTime()),
      patientMedicalHistory = readPatientMedicalHistory(record),
      ccEnc = readCCEncounter(record)
    )
  }

  private def readSubjectiveNodeId(record: Record): Int = record.get("subjectiveId").asInt()

  private def readObjective(record: Record): Objective = {
    Objective(
      id = record.get("objectiveId").asInt(),
      vital = record.get("vital").asString(),
      labTest = record.get("labTest").asString(),
      physicalExam = record.get("physicalExam").asString(),
      diagnosticData = record.get("diagnosticData").asString(),
      createdAt = Some(record.get("objectiveCreatedAt").asLocalDateTime())
    )
  }

  private def readAssessment(record: Record): Assessment = {
    Assessment(
      id = record.get("assessmentId").asInt(),
      ddx = record.get("ddx").asString(),
      mechanism = record.get("mechanism").asString(),
      createdAt = Some(record.get("assessmentCreatedAt").asLocalDateTime())
    )
  }

  private def readPlan(record: Record): Plan = {
    Plan(
      id = record.get("planId").asInt(),
      indication = record.get("indication").asString(),
      management = record.get("management").asString(),
      summary = record.get("summary").asString(),
      followup = record.get("followup").asString(),
      createdAt = Some(record.get("planCreatedAt").asLocalDateTime())
    )
  }

  private def readPatientSoapCreatedNode(record: Record): PatientSoap = {
    PatientSoap(
      id = record.get("soapId").asInt(),
      patientId = record.get("patientId").asInt(),
      subjectiveNodeData = null,
      objective = null,
      assessment = null,
      plan = null,
      createdAt = Some(record.get("soapCreatedAt").asLocalDateTime())
    )
  }

  private def readPatientSoapNode(record: Record): PatientSoap = {
    PatientSoap(
      id = record.get("soapId").asInt(),
      patientId = record.get("patientId").asInt(),
      subjectiveNodeData = readSubjectiveNodeData(record),
      objective = readObjective(record),
      assessment = readAssessment(record),
      plan = readPlan(record),
      createdAt = Some(record.get("soapCreatedAt").asLocalDateTime())
    )
  }


  private def readPico(record: Record): Pico = {
    Pico(
      id = record.get("picoId").asInt(),
      problem = record.get("problem").asString(),
      intervention = record.get("intervention").asString(),
      comparison = Option(record.get("comparison").asString("")),
      outcome = record.get("outcome").asString(),
      createdAt = Option(record.get("picoCreatedAt").asLocalDateTime()),
      searchQuery = Option(record.get("searchQuery").asString(null)),
      timePeriod = Option(record.get("timePeriod").asString(""))
    )
  }

  private def readArticle(record: Record): Article = {
    Article(
      id = Option(record.get("articleId").asLong()),
      title = record.get("title").asString(),
      authors = record.get("authors").asString(),
      journal = record.get("journal").asString(),
      pubDate = record.get("pubDate").asString(),
      abstractText = record.get("abstractText").asString(),
      summary = Option(record.get("summary").asString()),
      abstractComponent = Some(readAbstractComponent(record))
    )
  }

  private def readAbstractComponent(record: Record): AbstractComponent = {
    AbstractComponent(
      background = Option(record.get("background").asString()),
      objectives = Option(record.get("objectives").asString()),
      methods = Option(record.get("methods").asString()),
      result = Option(record.get("result").asString()),
      conclusion = Option(record.get("conclusion").asString()),
    )
  }

  private def readCnt(record: Record): Int = {
    record.get("cnt").asInt()
  }
}
