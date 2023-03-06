package models

import java.time.LocalDateTime

case class SubjectiveNodeData(id: Int, patientMedicalHistory: PatientMedicalHistory, ccEnc: CCEncounter, createdAt: Option[LocalDateTime]) extends Identifiable {
  override def toString: String = List(patientMedicalHistory, ccEnc).mkString(" ")
}
