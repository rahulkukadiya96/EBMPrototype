package models

case class FetchPicoRequest(comparison : Option[String], ids : List[Int], limit : Option[Int])
