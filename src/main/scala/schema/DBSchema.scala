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

    def address = column[String]("address")

    def email = column[String]("email")

    def password = column[String]("password")

    def createdDate = column[LocalDateTime]("CREATED_AT")

    def * = (id, name, age, address, email, password, createdDate).mapTo[Patient]
  }

  class CCEncounterTable(tag: Tag) extends Table[CCEncounter](tag, "CC_ENCOUNTER") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def signs = column[String]("SIGNS")

    def symptoms = column[String]("SYMPTOMS")

    def createdDate = column[LocalDateTime]("CREATED_AT")

    def subjectiveId = column[Int]("SUBJECTIVE_ID")

    def * = (id, subjectiveId, signs, symptoms, createdDate).mapTo[CCEncounter]

    def subjectiveIdFK = foreignKey("FK_CC_ENCOUNTER_SUBJECTIVE", subjectiveId, SubjectiveQuery)(_.id)
  }

  class PatientMedicalHistoryTable(tag: Tag) extends Table[PatientMedicalHistory](tag, "PMH") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def medications = column[String]("MEDICATIONS")

    def allergies = column[String]("ALLERGIES")

    def procedure = column[String]("PROCEDURE")

    def familyHistory = column[String]("FAMILY_HISTORY")

    def demographics = column[String]("DEMOGRAPHIC")

    def createdDate = column[LocalDateTime]("CREATED_AT")

    def subjectiveId = column[Int]("SUBJECTIVE_ID")

    override def * : ProvenShape[PatientMedicalHistory] = (id, subjectiveId, medications, allergies, procedure, familyHistory, demographics, createdDate).mapTo[PatientMedicalHistory]

    def subjectiveIdFK = foreignKey("FK_PMH_SUBJECTIVE", subjectiveId, SubjectiveQuery)(_.id)
  }

  class SubjectiveTable(tag: Tag) extends Table[Subjective](tag, "SUBJECTIVE") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def pmhId = column[Int]("PMH_ID")

    def ccEncId = column[Int]("CC_ENC_ID")

    def createdDate = column[LocalDateTime]("CREATED_AT")

    def * = (id, createdDate).mapTo[Subjective]
  }

  val PatientList = TableQuery[PatientTable]
  val CCEncounters = TableQuery[CCEncounterTable]
  val PatientMedicalHistoryQuery = TableQuery[PatientMedicalHistoryTable]
  val SubjectiveQuery = TableQuery[SubjectiveTable]


  private val databaseSetup = DBIO.seq(
    PatientList.schema.create,
    SubjectiveQuery.schema.create,
    CCEncounters.schema.create,
    PatientMedicalHistoryQuery.schema.create,
    PatientList forceInsertAll Seq(
      Patient(1, "David", 28, "ON", "david96", "david96", LocalDateTime of(2010, 8, 8, 8, 52)),
      Patient(2, "Rahul", 25, "BC", "rahul007", "rahul007", LocalDateTime of(2020, 8, 9, 5, 36)),
      Patient(3, "Terrace", 78, "MB", "terrace963", "terrace963", LocalDateTime of(2016, 8, 8, 1, 27))
    ),
    SubjectiveQuery forceInsertAll Seq(
      Subjective(1),
      Subjective(2),
    ),
    CCEncounters forceInsertAll Seq(
      CCEncounter(1, 1, "Abdominal Pain", "Stomach Infection"),
      CCEncounter(2, 2, "Coughing", "Viral Infection"),
    ),
    PatientMedicalHistoryQuery forceInsertAll Seq(
      PatientMedicalHistory(1, 1, "Paracetamol", "Skin", "Medication", " No family history", "Asian"),
      PatientMedicalHistory(2, 2, "Bitadin", "Skin", "Medication", " No family history", "Asian"),
    ),
  )


  def createDatabase: AppDAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new AppDAO(db)

  }
}
