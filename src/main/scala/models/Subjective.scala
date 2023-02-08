package models

import java.time.LocalDateTime

case class Subjective(id: Int,  createdAt: Option[LocalDateTime]) extends Identifiable
