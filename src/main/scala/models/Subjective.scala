package models

import java.time.LocalDateTime

case class Subjective(id: Int, createdAt: LocalDateTime = LocalDateTime.now()) extends Identifiable
