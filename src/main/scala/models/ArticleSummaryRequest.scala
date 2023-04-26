package models

import play.api.libs.json.{Json, OFormat}

case class ArticleSummaryRequest(id : Long, text : String, maxLength : Int)

object ArticleSummaryRequest {
  implicit val format: OFormat[ArticleSummaryRequest] = Json.format[ArticleSummaryRequest]
}

case class ArticleSummaryRequestData(articles : Seq[ArticleSummaryRequest])
object ArticleSummaryRequestData {
  implicit val format: OFormat[ArticleSummaryRequestData] = Json.format[ArticleSummaryRequestData]
}

case class ArticleSummaryResponse(id : Long, text : String, summary : String)

object ArticleSummaryResponse {
  implicit val format: OFormat[ArticleSummaryResponse] = Json.format[ArticleSummaryResponse]
}

case class SummaryResponse(data : Seq[ArticleSummaryResponse], success : Boolean)
object SummaryResponse {
  implicit val format: OFormat[SummaryResponse] = Json.format[SummaryResponse]
}
