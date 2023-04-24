package models

import sangria.validation.Violation

case object InvalidPDFCoerceViolation extends Violation {
  override def errorMessage: String = "Error during parsing PDF"
}
