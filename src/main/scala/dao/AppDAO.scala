package dao

import models._
import org.neo4j.driver.v1.{Driver, Record}
import utility.DateTimeFormatUtil.getCurrentUTCTime

import java.time.LocalDateTime
import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters
import scala.concurrent.Future

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

  def buildRelationForSubjectNode(patientId: Int, subId: Int, patMedId: Int, ccEncId: Int): Future[SubjectiveNodeData] = {
    var queryString = s"MATCH (patient: Patient) WHERE ID(patient) = ${patientId} "
    queryString += s"MATCH (subjective: Subjective) WHERE ID(subjective) = ${subId} "
    queryString += s"MATCH (patientMedicalHistory: PatientMedicalHistory ) WHERE ID(patientMedicalHistory) = ${patMedId} "
    queryString += s"MATCH (ccEnc: CCEncounter ) WHERE ID(ccEnc) = ${ccEncId} "


    queryString += s" CREATE (patient)-[:soap_subject { subId : ${subId}  }]->(subjective), "
    queryString += s" (subjective)-[:patient {patId : ${patientId} }]->(patient), "

    queryString += s" (subjective)-[:pmh { patMedId : ${patMedId} }]->(patientMedicalHistory),"
    queryString += s" (patientMedicalHistory)-[:subId { subId : ${subId} }]->(subjective), "

    queryString += s" (subjective)-[:ccEnc { ccEnc : ${ccEncId} }]->(ccEnc), "
    queryString += s" (ccEnc)-[:subId { subId : ${subId} }]->(subjective) "

    queryString += s" RETURN ID(subjective) as subjectiveId, subjective.createdAt as createdAt, "
    queryString += s" ID(patientMedicalHistory) as patientMedicalHistoryId, patientMedicalHistory.medications as medications, patientMedicalHistory.allergies as allergies, patientMedicalHistory.procedure as procedure, patientMedicalHistory.familyHistory as familyHistory, patientMedicalHistory.demographics as demographics, patientMedicalHistory.createdAt as patientMedicalHistoryCreatedAt, "
    queryString += s" ID(ccEnc) as ccEncId, ccEnc.signs as signs, ccEnc.symptoms as symptoms, ccEnc.createdAt as ccEncCreatedAt"

    writeData(queryString, readSubjectiveNodeData)
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

  def getSubjectiveData(ids: Seq[Int]): Future[Seq[SubjectiveNodeData]] = {
    var queryString = s"MATCH (subjective:Subjective) -[:pmh]-> (patientMedicalHistory:PatientMedicalHistory), (subjective) -[:ccEnc] -> (ccEnc:CCEncounter) "
    queryString += s" WHERE ID(subjective) IN [${ids.mkString(",")}]"
    queryString += " RETURN ID(subjective) as subjectiveId, subjective.createdAt as createdAt, "
    queryString += " ID(patientMedicalHistory) as patientMedicalHistoryId, patientMedicalHistory.medications as medications, patientMedicalHistory.allergies as allergies, patientMedicalHistory.procedure as procedure, patientMedicalHistory.familyHistory as familyHistory, patientMedicalHistory.demographics as demographics, patientMedicalHistory.createdAt as patientMedicalHistoryCreatedAt, "
    queryString += " ID(ccEnc) as ccEncId, ccEnc.signs as signs, ccEnc.symptoms as symptoms, ccEnc.createdAt as ccEncCreatedAt"
    getData(queryString, readSubjectiveNodeData)
  }

  /* CCEnc methods */

  def getCCEncList: Future[Seq[CCEncounter]] = {
    val queryString = s"MATCH (n: CCEncounter) RETURN ID(n) as ccEncId, n.signs as signs, n.symptoms as symptoms, n.createdAt as ccEncCreatedAt"
    getData(queryString, readCCEncounter)
  }

  def getCCEncounter(ids: Seq[Int]): Future[Seq[CCEncounter]] = {
    val queryString = s"MATCH (n: CCEncounter) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as ccEncId, n.signs as signs, n.symptoms as symptoms, n.createdAt as ccEncCreatedAt"
    getData(queryString, readCCEncounter)
  }

  def getCCEncounterBySubjective(ids: Seq[Int]): Future[Seq[CCEncounter]] = {
    //    val queryString = s"MATCH (n: CCEncounter) WHERE n.subjectiveId IN [${ids.mkString(",")}] RETURN ID(n) as id, n.subjectiveId as subjectiveId, n.signs as signs, n.symptoms as symptoms, n.createdAt as createdAt"
    val queryString = s"MATCH (n: Subjective) <- [ : ]"
    getData(queryString, readCCEncounter)
  }

  /* Patient Medical History methods */
  def getPatientMedicalHistory(ids: Seq[Int]): Future[Seq[PatientMedicalHistory]] = {
    val queryString = s"MATCH (n: PatientMedicalHistory) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as patientMedicalHistoryId, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory = familyHistory, n.demographics as demographics, n.createdAt as patientMedicalHistoryCreatedAt"
    getData(queryString, readPatientMedicalHistory)
  }

  def getPatientMedicalHistory: Future[Seq[PatientMedicalHistory]] = {
    val queryString = s"MATCH (n: PatientMedicalHistory) RETURN ID(n) as patientMedicalHistoryId, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory as familyHistory, n.demographics as demographics, n.createdAt as patientMedicalHistoryCreatedAt"
    getData(queryString, readPatientMedicalHistory)
  }

  def getPatientMedicalHistoryBySubjective(ids: Seq[Int]): Future[Seq[PatientMedicalHistory]] = {
    val queryString = s"MATCH (n: PatientMedicalHistory) WHERE n.subjectiveId IN [${ids.mkString(",")}] RETURN ID(n) as patientMedicalHistoryId, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory as familyHistory, n.demographics as demographics, n.createdAt as patientMedicalHistoryCreatedAt"
    getData(queryString, readPatientMedicalHistory)
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

    FutureConverters.toScala(queryCompletion)
  }

  private def getData[T](query: String, reader: Record => T) = {
    val session = connection.session()
    val queryCompletion = session
      .runAsync(query)
      .thenCompose[java.util.List[T]](c => c.listAsync[T](record => reader(record)))
      .thenApply[Seq[T]] {
        _.asScala
      }
      .whenComplete((_, _) => session.closeAsync())

    FutureConverters.toScala(queryCompletion)
  }

  def getTodayDateTimeNeo4j(localDateTime: LocalDateTime): String = {
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
}
