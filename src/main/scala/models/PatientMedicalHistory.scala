package models

import java.time.LocalDateTime

case class PatientMedicalHistory(id : Int, medications: String, allergies: String, procedure: String, familyHistory: String, demographics: String, createdAt: Option[LocalDateTime]) extends Identifiable
