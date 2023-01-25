package models

import java.time.LocalDateTime
import java.time.LocalDateTime.now

case class CCEncounter(id : Int, signs : String, symptoms : String, createdAt: LocalDateTime = now()) extends Identifiable
