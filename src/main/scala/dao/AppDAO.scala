package dao

import models.{AuthProviderSignupData, CCEncounter, Patient, PatientMedicalHistory, Subjective}
import schema.DBSchema.{CCEncounters, PatientList, PatientMedicalHistoryQuery, SubjectiveQuery}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class AppDAO(db: Database) {
  def patientLst: Future[Seq[Patient]] = db.run(PatientList.result)

  def createPatient(name: String, age: Int, address: String, authProvider: AuthProviderSignupData): Future[Patient] = {
    val newUser = Patient(0, name, age, address, authProvider.email.email, authProvider.email.password)
    val insertAndReturnUserQuery = (PatientList returning PatientList.map(_.id)) into {
      (user, id) => user.copy(id = id)
    }

    println(s"New User ::: $newUser")

    db.run(
      insertAndReturnUserQuery += newUser
    )
  }
  /*def getPatient(id: Int): Future[Option[Patient]] = db.run(
    patientList.filter(_.id === id).result.headOption
  )*/

  def getPatients(id: Seq[Int]): Future[Seq[Patient]] = db.run(
    PatientList.filter(_.id inSet id).result
  )

  def getCCEncList: Future[Seq[CCEncounter]] = db.run(CCEncounters.result)

  def getCCEncounter(id: Seq[Int]): Future[Seq[CCEncounter]] = db.run(
    CCEncounters.filter(_.id inSet id).result
  )

  def getCCEncounterBySubjective(ids: Seq[Int]): Future[Seq[CCEncounter]] = {
    db.run {
      CCEncounters.filter(_.subjectiveId inSet ids).result
    }
  }

  def getPatientMedicalHistoryList: Future[Seq[PatientMedicalHistory]] = db.run(PatientMedicalHistoryQuery.result)

  def getPatientMedicalHistory(id: Seq[Int]): Future[Seq[PatientMedicalHistory]] = db.run(
    PatientMedicalHistoryQuery.filter(_.id inSet id).result
  )

  def getPatientMedicalHistoryBySubjective(ids: Seq[Int]): Future[Seq[PatientMedicalHistory]] = {
    db.run {
      PatientMedicalHistoryQuery.filter(_.subjectiveId inSet ids).result
    }
  }

  def getSubjectiveList: Future[Seq[Subjective]] = db.run(SubjectiveQuery.result)

  def getSubject(id: Seq[Int]): Future[Seq[Subjective]] = db.run(
    SubjectiveQuery.filter(_.id inSet id).result
  )
}
