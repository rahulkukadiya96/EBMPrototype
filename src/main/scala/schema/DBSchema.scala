package schema

import dao.AppDAO
import models.Patient
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * file defines what we want to expose.
 * There are defined types (from GraphQL point of view) and shape of the schema a client is able to query for.
 */
object DBSchema {
  class PatientTable(tag: Tag) extends Table[Patient](tag, "PATIENT") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def age = column[Int]("age")

    def * = (id, name, age).mapTo[Patient]
  }

  val patientList = TableQuery[PatientTable]

  private val databaseSetup = DBIO.seq(
    patientList.schema.create,
    patientList forceInsertAll Seq(
      Patient(1, "David", 28),
      Patient(2, "Rahul", 25),
      Patient(3, "Terrace", 78)
    )
  )


  def createDatabase: AppDAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new AppDAO(db)

  }
}
