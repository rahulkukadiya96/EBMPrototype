package models

import java.time.LocalDateTime

case class Assessment(id: Int, ddx: String, mechanism: String, createdAt: Option[LocalDateTime]) extends Identifiable
