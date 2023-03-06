package models

import java.time.LocalDateTime

case class CCEncounter(id: Int, signs: String, symptoms: String, createdAt: Option[LocalDateTime]) extends Identifiable {
  override def toString: String = List(signs, symptoms).mkString(" ")
}
