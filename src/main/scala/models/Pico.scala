package models

case class Pico(id: Int, problem: String, intervention: String, comparison: Option[String], outcome: String) extends Identifiable
