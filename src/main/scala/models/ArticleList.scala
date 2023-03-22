package models

case class ArticleListResponse(status : Int, message : Option[String], totalPages : Option[Int], data: Option[Seq[Article]])
