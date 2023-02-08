package models

import utility.DateTimeFormatUtil.getCurrentUTCTime

import java.time.LocalDateTime

case class Subjective(id: Int,  createdAt: LocalDateTime = getCurrentUTCTime) extends Identifiable
