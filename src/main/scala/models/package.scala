import sangria.validation.Violation

import java.time.LocalDateTime

package object models {
  case class Patient(id: Int, name: String, age: Int, createdAt: LocalDateTime)

  case object LocalDateTimeCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing DateTime"
  }
}
