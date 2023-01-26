package models

import utility.DateTimeFormatUtil.getCurrentUTCTime

import java.time.LocalDateTime

case class Patient(id: Int, name: String, age: Int, createdAt: LocalDateTime = getCurrentUTCTime) extends Identifiable