package models

import utility.DateTimeFormatUtil.getCurrentUTCTime

import java.time.LocalDateTime

case class Patient(id: Int, name: String, age: Int, address: String, createdAt: LocalDateTime = getCurrentUTCTime) extends Identifiable