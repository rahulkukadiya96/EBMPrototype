package models

import java.time.LocalDateTime

case class Objective(id: Int, vital: String, labTest: String, physicalExam: String, diagnosticData: String, createdAt: Option[LocalDateTime]) extends Identifiable
