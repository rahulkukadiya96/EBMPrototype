package models

import play.api.libs.json.{Json, OFormat, Writes}

case class ArticleSummaryRequest(id: Long, text: String, maxLength: Int)

object ArticleSummaryRequest {
  implicit val format: OFormat[ArticleSummaryRequest] = Json.format[ArticleSummaryRequest]
}

case class ArticleSummaryRequestData(articles: Seq[ArticleSummaryRequest])

object ArticleSummaryRequestData {
  implicit val format: OFormat[ArticleSummaryRequestData] = Json.format[ArticleSummaryRequestData]
}

case class ArticleSummaryResponse(id: Long, text: String, summary: String, score : String)

object ArticleSummaryResponse {
  implicit val format: OFormat[ArticleSummaryResponse] = Json.format[ArticleSummaryResponse]
}

case class SummaryResponse(data: Seq[ArticleSummaryResponse], success: Boolean)

object SummaryResponse {
  implicit val format: OFormat[SummaryResponse] = Json.format[SummaryResponse]
}

case class RougeScores(rouge1: RougeScore, rouge2: RougeScore, rougel: RougeScore)

object RougeScores {
  implicit val format: OFormat[RougeScores] = Json.format[RougeScores]
  implicit val writes: Writes[RougeScores] = Json.writes[RougeScores]

}

case class RougeScore(r: Double, p: Double, f: Double)
object RougeScore {
  implicit val format: OFormat[RougeScore] = Json.format[RougeScore] /* From JSON String */
  implicit val writes: Writes[RougeScore] = Json.writes[RougeScore] /* To JSON String */
}