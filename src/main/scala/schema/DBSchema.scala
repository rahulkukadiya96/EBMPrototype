package schema

import dao.AppDAO
import models.Patient
import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType

import java.sql.Timestamp
import java.sql.Timestamp.valueOf
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * file defines what we want to expose.
 * There are defined types (from GraphQL point of view) and shape of the schema a client is able to query for.
 */
object DBSchema {
  /**
   * This custom mapper will convert LocalDateTime into Long, which is a primitive recognized by H2.
   */
  implicit val localDateTimeColumnType: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
    valueOf,
    _.toInstant atZone systemDefault toLocalDateTime
  )

  class PatientTable(tag: Tag) extends Table[Patient](tag, "PATIENT") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def age = column[Int]("age")

    def createdDate = column[LocalDateTime]("CREATED_AT")

    def * = (id, name, age, createdDate).mapTo[Patient]
  }

  val patientList = TableQuery[PatientTable]

  private val databaseSetup = DBIO.seq(
    patientList.schema.create,
    patientList forceInsertAll Seq(
      Patient(1, "David", 28, LocalDateTime of(2010, 20, 8, 12, 52)),
      Patient(2, "Rahul", 25, LocalDateTime of(2020, 29, 9, 5, 36)),
      Patient(3, "Terrace", 78, LocalDateTime of(2016, 21, 12, 1, 27))
    )
  )


  def createDatabase: AppDAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new AppDAO(db)

  }
}
