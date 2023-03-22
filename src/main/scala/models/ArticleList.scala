package models

case class ArticleListResponse(status : Int, message : Option[String], count : Option[Int], data: Option[Seq[Article]])
