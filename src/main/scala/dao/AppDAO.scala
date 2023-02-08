package dao

import models._
import org.neo4j.driver.v1.{Driver, Record}

import java.time.LocalDateTime
import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters
import scala.concurrent.Future

class AppDAO(connection: Driver) {
  def patientList: Future[Seq[Patient]] = {
    val queryString = "MATCH (n: Patient) RETURN ID(n) as id, n.name as name, n.age as age, n.address as address, n.createAt as createAt"
    getData(queryString, readPatient)
  }

  def getPatients(ids: Seq[Int]): Future[Seq[Patient]] = {
    val queryString = s"MATCH (n: Patient) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as id, n.name as name, n.age as age, n.address as address, n.createAt as createAt"
    getData(queryString, readPatient)
  }


  def createPatient(patient: Patient): Future[Patient] = {
    val queryString = s"CREATE (patient : Patient{name : '${patient.name}', age : ${patient.age}, address : '${patient.address}', createAt : ${getTodayDateTimeNeo4j(patient.createdAt)} }) RETURN ID(patient) as id, patient.name as name, patient.age as age, patient.address as address, patient.createAt as createAt"
    writeData(queryString, readPatient)
  }


  def createCCEnc(ccEnc: CCEncounter): Future[CCEncounter] = {
    val queryString = s"CREATE (n : CCEncounter{signs : '${ccEnc.signs}', symptoms : '${ccEnc.symptoms}', createdAt : ${getTodayDateTimeNeo4j(ccEnc.createdAt)} }) RETURN ID(n) as id, n.signs as signs, n.symptoms as symptoms, n.createdAt as createdAt"
    writeData(queryString, readCCEncounter)
  }

  def createSubjectNode(patientId: Int, ccEnc: CCEncounter, patientMedicalHistory: PatientMedicalHistory, subjective: Subjective): Future[Subjective] = {
    var queryString = s"CREATE (ccEnc : CCEncounter{signs : '${ccEnc.signs}', symptoms : '${ccEnc.symptoms}', createdAt : ${getTodayDateTimeNeo4j(ccEnc.createdAt)} }) "
    queryString += s", (patientMedicalHistory : PatientMedicalHistory{medications : '${patientMedicalHistory.medications}', allergies : '${patientMedicalHistory.allergies}', procedure : '${patientMedicalHistory.procedure}', familyHistory : '${patientMedicalHistory.familyHistory}', demographics : '${patientMedicalHistory.demographics}',createAt : ${getTodayDateTimeNeo4j(patientMedicalHistory.createdAt)} }) "
    queryString += s", (subjective : Subjective{ createdAt : ${getTodayDateTimeNeo4j(subjective.createdAt)} }) "

    queryString += "RETURN ID(ccEnc) as ccEncId, ccEnc.signs as signs, ccEnc.symptoms as symptoms, ccEnc.createdAt as ccEncCreatedAt "
    queryString += ", ID(patientMedicalHistory) as patientMedicalHistoryId, patientMedicalHistory.medications as medications, patientMedicalHistory.allergies as allergies, patientMedicalHistory.procedure as procedure, patientMedicalHistory.familyHistory = familyHistory, patientMedicalHistory.demographics as demographics, patientMedicalHistory.createdAt as patientMedicalHistoryCreatedAt"
    queryString += ", ID(subjective) as id, subjective.createdAt as createdAtSubjective"
    writeData(queryString, readSubjective)
  }

  def buildRelationForSubjectNode(patientId: Int, subId: Int, patMedId: Int, ccEncId: Int): Future[Subjective] = {
    var queryString = s"MATCH (patient: Patient {id: ${patientId} }, subjective: Subjective {id: ${subId} },patientMedicalHistory: PatientMedicalHistory {id: ${patMedId} },ccEnc: CCEncounter {id: ${ccEncId} }) "

    queryString += s" create (patient)-[:soap_subject { subId : ${subId}]->(subjective) "
    queryString += s" create (subjective)-[:patient {patId : ${patientId}]->(patient) "

    queryString += s" create (subjective)-[:pmh { patMedId : ${patMedId}]->(patientMedicalHistory) "
    queryString += s" create (patientMedicalHistory)-[:subId { subId : ${subId}]->(subjective) "

    queryString += s" create (subjective)-[:ccEnc { ccEnc : ${ccEncId}]->(ccEnc) "
    queryString += s" create (ccEnc)-[:subId { subId : ${subId}]->(subjective) "

    writeData(queryString, readSubjective)
  }

  def getCCEncList: Future[Seq[CCEncounter]] = {
    val queryString = s"MATCH (n: CCEncounter) RETURN ID(n) as id, n.signs as signs, n.symptoms as symptoms, n.createdAt as createdAt"
    getData(queryString, readCCEncounter)
  }

  def getCCEncounter(ids: Seq[Int]): Future[Seq[CCEncounter]] = {
    val queryString = s"MATCH (n: CCEncounter) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as id, n.signs as signs, n.symptoms as symptoms, n.createdAt as createdAt"
    getData(queryString, readCCEncounter)
  }

  def getCCEncounterBySubjective(ids: Seq[Int]): Future[Seq[CCEncounter]] = {
    //    val queryString = s"MATCH (n: CCEncounter) WHERE n.subjectiveId IN [${ids.mkString(",")}] RETURN ID(n) as id, n.subjectiveId as subjectiveId, n.signs as signs, n.symptoms as symptoms, n.createdAt as createdAt"
    val queryString = s"MATCH (n: Subjective) <- [ : ]"
    getData(queryString, readCCEncounter)
  }

  def getPatientMedicalHistory: Future[Seq[PatientMedicalHistory]] = {
    val queryString = s"MATCH (n: PatientMedicalHistory) RETURN ID(n) as id, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory = familyHistory, n.demographics as demographics, n.createdAt as createdAt"
    getData(queryString, readPatientMedicalHistory)
  }

  def getPatientMedicalHistory(ids: Seq[Int]): Future[Seq[PatientMedicalHistory]] = {
    val queryString = s"MATCH (n: PatientMedicalHistory) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as id, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory = familyHistory, n.demographics as demographics, n.createdAt as createdAt"
    getData(queryString, readPatientMedicalHistory)
  }

  def createPatientMedicalHistory(patientMedicalHistory: PatientMedicalHistory): Future[PatientMedicalHistory] = {
    val queryString = s"CREATE (n : PatientMedicalHistory{medications : '${patientMedicalHistory.medications}', allergies : '${patientMedicalHistory.allergies}', procedure : '${patientMedicalHistory.procedure}', familyHistory : '${patientMedicalHistory.familyHistory}', demographics : '${patientMedicalHistory.demographics}',createAt : ${getTodayDateTimeNeo4j(patientMedicalHistory.createdAt)} }) RETURN ID(n) as id, n.subjectiveId as subjectiveId, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory = familyHistory, n.demographics as demographics, n.createdAt as createdAt"
    writeData(queryString, readPatientMedicalHistory)
  }

  def getPatientMedicalHistoryBySubjective(ids: Seq[Int]): Future[Seq[PatientMedicalHistory]] = {
    val queryString = s"MATCH (n: PatientMedicalHistory) WHERE n.subjectiveId IN [${ids.mkString(",")}] RETURN ID(n) as id, n.medications as medications, n.allergies as allergies, n.procedure as procedure, n.familyHistory = familyHistory, n.demographics as demographics, n.createdAt as createdAt"
    getData(queryString, readPatientMedicalHistory)
  }

  def createSubject(subjective: Subjective): Future[Subjective] = {
    val queryString = s"CREATE (subjective : Subjective{ createdAt : ${getTodayDateTimeNeo4j(subjective.createdAt)} }) RETURN ID(subjective) as id, subjective.createdAt as createdAt"
    writeData(queryString, readSubjective)
  }

  def getSubjectiveList: Future[Seq[Subjective]] = {
    val queryString = s"MATCH (n: Subjective) RETURN ID(n) as id, n.createdAt as createdAt"
    getData(queryString, readSubjective)
  }

  def getSubject(ids: Seq[Int]): Future[Seq[Subjective]] = {
    val queryString = s"MATCH (n: Subjective) WHERE ID(n) IN [${ids.mkString(",")}] RETURN ID(n) as id, n.createdAt as createdAt"
    getData(queryString, readSubjective)
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
      id = record.get("id").asInt(),
      //      subjectiveId = record.get("subjectiveId").asInt(),
      signs = record.get("signs").asString(),
      symptoms = record.get("symptoms").asString(),
      createdAt = record.get("createdAt").asLocalDateTime()
    )
  }

  private def readPatientMedicalHistory(record: Record): PatientMedicalHistory = {
    PatientMedicalHistory(
      id = record.get("id").asInt(),
      //      subjectiveId = record.get("subjectiveId").asInt(),
      medications = record.get("signs").asString(),
      allergies = record.get("symptoms").asString(),
      procedure = record.get("procedure").asString(),
      familyHistory = record.get("familyHistory").asString(),
      demographics = record.get("demographics").asString(),
      createdAt = record.get("createdAt").asLocalDateTime()
    )
  }

  private def readSubjective(record: Record): Subjective = {
    Subjective(
      id = record.get("id").asInt(),
      createdAt = record.get("createdAt").asLocalDateTime()
    )
  }
}
