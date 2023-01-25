package models

import sangria.validation.Violation

case object LocalDateTimeCoerceViolation extends Violation {
  override def errorMessage: String = "Error during parsing DateTime"
}
