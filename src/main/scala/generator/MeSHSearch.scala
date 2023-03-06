package generator

import generator.ExternalCallUtils.{callApi, extractIdFromXml}
import schema.DBSchema.config

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem

object MeSHSearch {
  val BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/"
  val MESH_DB_NAME = "mesh"
  val API_KEY = s"&api_key=${config.getString("ncbi_api_key")}"

  def searchSubjectHeading(text: Seq[String], queryBuilder: Seq[String] => Future[String]): Future[Option[String]] = {
    text.isEmpty match {
      case false =>
        val terms = text.mkString(",")
        for {
          ids <- callApi(s"$BASE_URL/esearch.fcgi?db=$MESH_DB_NAME&term=$terms$API_KEY", extractIdFromXml)
          summaries <- fetchSummaryData(ids.mkString(","))
          query <- queryBuilder(summaries)
        } yield {
          //      println(s"ids is $ids")
          //      println(s"summaries is $summaries")
          println(s"Query is $query")
          Option(query)
        }

      case true =>
        Future {
          Option.empty
        }
    }
  }

  private def fetchSummaryData(ids: String) = callApi(s"$BASE_URL/esummary.fcgi?db=$MESH_DB_NAME&id=$ids$API_KEY", extractHeadingFromXml)

  def extractHeadingFromXml(xml: Elem): Seq[String] = {
    val value = (xml \\ "Item").filter(item => (item \@ "Name") == "DS_MeshTerms").flatMap(_.child).map(_.text.trim).filter(_.nonEmpty).flatMap(_.split(",").toSeq)
    value
  }
}
