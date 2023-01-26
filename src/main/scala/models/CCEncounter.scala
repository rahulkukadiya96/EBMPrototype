package models

import utility.DateTimeFormatUtil.getCurrentUTCTime

import java.time.LocalDateTime
import java.time.LocalDateTime.now

case class CCEncounter(id : Int, subjectiveId: Int, signs : String, symptoms : String, createdAt: LocalDateTime = getCurrentUTCTime) extends Identifiable
