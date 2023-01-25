package schema

import dao.AppDAO
import models.{CCEncounter, Patient, PatientMedicalHistory, Subjective}
import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType
import slick.lifted.ProvenShape

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

  class CCEncounterTable(tag: Tag) extends Table[CCEncounter](tag, "CC_ENCOUNTER") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def signs = column[String]("signs")

    def symptoms = column[String]("symptoms")

    def createdDate = column[LocalDateTime]("CREATED_AT")

    def * = (id, signs, symptoms, createdDate).mapTo[CCEncounter]
  }

  class PatientMedicalHistoryTable(tag: Tag) extends Table[PatientMedicalHistory](tag, "PMH") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def medications = column[String]("medications")

    def allergies = column[String]("allergies")

    def procedure = column[String]("procedure")

    def familyHistory = column[String]("familyHistory")

    def demographics = column[String]("demographics")

    def createdDate = column[LocalDateTime]("CREATED_AT")

    override def * : ProvenShape[PatientMedicalHistory] = (id, medications, allergies, procedure, familyHistory, demographics, createdDate).mapTo[PatientMedicalHistory]
  }

  class SubjectiveTable(tag: Tag) extends Table[Subjective](tag, "SUBJECTIVE") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def pmhId = column[Int]("PMH_ID")

    def ccEncId = column[Int]("CC_ENC_ID")

    def createdDate = column[LocalDateTime]("CREATED_AT")

    def * = (id, pmhId, ccEncId, createdDate).mapTo[Subjective]
  }

  val patientList = TableQuery[PatientTable]
  val ccEncounters = TableQuery[CCEncounterTable]
  val patientMedicalHistory = TableQuery[PatientMedicalHistoryTable]
  val subjective = TableQuery[SubjectiveTable]


  private val databaseSetup = DBIO.seq(
    patientList.schema.create,
    patientList forceInsertAll Seq(
      Patient(1, "David", 28, LocalDateTime of(2010, 8, 8, 8, 52)),
      Patient(2, "Rahul", 25, LocalDateTime of(2020, 8, 9, 5, 36)),
      Patient(3, "Terrace", 78, LocalDateTime of(2016, 8, 8, 1, 27))
    ),
    ccEncounters.schema.create,
    ccEncounters forceInsertAll Seq(
      CCEncounter(1, "Abdominal Pain", "Stomach Infection"),
      CCEncounter(2, "Coughing", "Viral Infection"),
    ),
    patientMedicalHistory.schema.create,
    patientMedicalHistory forceInsertAll Seq(
      PatientMedicalHistory(1, "Paracetamol", "Skin", "Medication", " No family history", "Asian"),
      PatientMedicalHistory(2, "Bitadin", "Skin", "Medication", " No family history", "Asian"),
    ),
    subjective.schema.create,
    subjective forceInsertAll Seq(
      Subjective(1, 1, 1),
      Subjective(2, 1, 2),
    )
  )


  def createDatabase: AppDAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new AppDAO(db)

  }
}
