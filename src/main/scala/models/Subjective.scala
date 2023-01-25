package models

import java.time.LocalDateTime

case class Subjective(id: Int, pmhId: Int, ccEncId: Int, createdAt: LocalDateTime = LocalDateTime.now()) extends Identifiable
