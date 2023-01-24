package dao

import schema.DBSchema
import schema.DBSchema.patientList
import slick.jdbc.H2Profile.api._

class AppDAO(db: Database) {
  def patientLst = db.run(patientList.result)
}
