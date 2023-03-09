package models

case class Response(data: Option[Seq[Int]], status : Int, message : Option[String], queryString : Option[String])
