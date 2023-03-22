package models

import java.time.LocalDateTime

case class Pico(id: Int, problem: String, intervention: String, comparison: Option[String], outcome: String, timePeriod: Option[String], searchQuery: Option[String], createdAt: Option[LocalDateTime]) extends Identifiable
