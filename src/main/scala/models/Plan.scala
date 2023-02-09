package models

import java.time.LocalDateTime

case class Plan(id: Int, indication: String, management: String, summary: String, followup: String, createdAt: Option[LocalDateTime]) extends Identifiable