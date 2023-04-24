package models

case class Article(id: Option[Long], title: String, authors :String, journal : String, pubDate : String, abstractText : String, summary : Option[String], abstractComponent : Option[AbstractComponent])
