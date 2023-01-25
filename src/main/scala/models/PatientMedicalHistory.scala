package models

import java.time.LocalDateTime
import java.time.LocalDateTime.now

case class PatientMedicalHistory(id : Int, medications: String, allergies: String, procedure: String, familyHistory: String, demographics: String, createdAt: LocalDateTime = now()) extends Identifiable
