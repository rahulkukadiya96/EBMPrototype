package dao

import models.Patient
import schema.DBSchema.patientList
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
}
