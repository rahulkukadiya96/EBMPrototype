package dao

import models.{CCEncounter, Patient, PatientMedicalHistory, Subjective}
import schema.DBSchema.{ccEncounters, patientList, patientMedicalHistory, subjective}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class AppDAO(db: Database) {
  def patientLst: Future[Seq[Patient]] = db.run(patientList.result)

  /*def getPatient(id: Int): Future[Option[Patient]] = db.run(
    patientList.filter(_.id === id).result.headOption
  )*/

  def getPatients(id: Seq[Int]): Future[Seq[Patient]] = db.run(
    patientList.filter(_.id inSet id).result
  )

  def getCCEncList: Future[Seq[CCEncounter]] = db.run(ccEncounters.result)

  def getCCEncounter(id: Seq[Int]): Future[Seq[CCEncounter]] = db.run(
    ccEncounters.filter(_.id inSet id).result
  )

  def getCCEncounterBySubjective(ids: Seq[Int]): Future[Seq[CCEncounter]] = {
    db.run {
      ccEncounters.filter(_.subjectiveId inSet ids).result
    }
  }

  def getPatientMedicalHistoryList: Future[Seq[PatientMedicalHistory]] = db.run(patientMedicalHistory.result)

  def getPatientMedicalHistory(id: Seq[Int]): Future[Seq[PatientMedicalHistory]] = db.run(
    patientMedicalHistory.filter(_.id inSet id).result
  )

  def getPatientMedicalHistoryBySubjective(ids: Seq[Int]): Future[Seq[PatientMedicalHistory]] = {
    db.run {
      patientMedicalHistory.filter(_.subjectiveId inSet ids).result
    }
  }

  def getSubjectiveList: Future[Seq[Subjective]] = db.run(subjective.result)

  def getSubject(id: Seq[Int]): Future[Seq[Subjective]] = db.run(
    subjective.filter(_.id inSet id).result
  )
}
