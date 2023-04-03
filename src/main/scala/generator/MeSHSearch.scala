package generator

import generator.ExternalCallUtils.{callApi, extractIdFromXml, urlEncode}
import schema.DBSchema.config

import scala.Option.empty
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
        val terms = text.map(urlEncode).mkString(",")
        for {
          ids <- callApi(s"$BASE_URL/esearch.fcgi?db=$MESH_DB_NAME&term=$terms$API_KEY", extractIdFromXml)
          summaries <- fetchSummaryData(ids)
          query <- queryBuilder(summaries)
        } yield {
          Option(query)
        }

      case true =>
        Future {
          empty
        }
    }
  }

  private def fetchSummaryData(ids: Seq[String]): Future[Seq[String]] = Option(ids) match {
    case Some(idList) =>
      callApi(s"$BASE_URL/esummary.fcgi?db=$MESH_DB_NAME&id=${idList.map(urlEncode).mkString(",")}$API_KEY", extractHeadingFromXml)
    case None => Future {
      Seq.empty
    }
  }

  def extractHeadingFromXml(xml: Elem): Future[Seq[String]] = Future {
    val value = (xml \\ "Item").filter(item => (item \@ "Name") == "DS_MeshTerms").flatMap(_.child).map(_.text.trim).filter(_.nonEmpty).flatMap(_.split(",").toSeq)
    value
  }
}
