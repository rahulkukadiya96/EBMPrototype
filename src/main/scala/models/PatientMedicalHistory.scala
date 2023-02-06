package models

import utility.DateTimeFormatUtil.getCurrentUTCTime

import java.time.LocalDateTime

case class PatientMedicalHistory(id : Int, subjectiveId: Int, medications: String, allergies: String, procedure: String, familyHistory: String, demographics: String, createdAt: LocalDateTime = getCurrentUTCTime) extends Identifiable
