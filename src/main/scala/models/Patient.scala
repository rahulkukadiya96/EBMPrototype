package models

import java.time.LocalDateTime
import java.time.LocalDateTime.now

case class Patient(id: Int, name: String, age: Int, createdAt: LocalDateTime = now()) extends Identifiable