package models

import java.time.LocalDateTime

case class PatientSoap(id: Int, patientId: Int, subjectiveNodeData: SubjectiveNodeData, objective: Objective, assessment: Assessment, plan: Plan , createdAt: Option[LocalDateTime]) extends Identifiable {
  override def toString: String = List(subjectiveNodeData, objective, assessment).mkString(" ")
}
