package generator

import models.Pico
import parser.CustomXMLParser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Using
import scala.xml.XML

object PubMedSearch {

  def fetchData(pico: Pico, retMax: Int, email: String = "rkukadiy@lakeheadu.ca"): Future[Seq[String]] = Future {
    val baseUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/"
//    val query = buildQuery(pico)
    val query = "cancer"
    val url = s"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=$query&retmax=$retMax"
    Using(Source.fromURL(url)) {
      data =>
        try {
          //        print(s"Data is ${data.mkString}")
          val xml = CustomXMLParser.loadString(data.mkString)
          val datas = (xml \\ "IdList" \\ "Id").map(_.text)
          print(s"Data is $datas")
          if ((xml \\ "ErrorList" \\ "Error").nonEmpty) {
            throw new RuntimeException(s"PubMed API error: ${(xml \\ "ErrorList" \\ "Error" \ "Message").text}")
          } else {
            (xml \\ "IdList" \\ "Id").map(_.text)
          }
        } catch {
          case e : Exception => {
            e.printStackTrace()
            Seq.empty
          }
        }
    }.getOrElse {
      Seq.empty
    }
  }


  /*def searchAll(pico: Pico, email: String): Future[Seq[String]] = {
    val retmax = 10000
    var ids = Seq.empty[String]
    var page = 0
    var total = 0

    def fetchDataPage(): Future[Seq[String]] = {
      val start = page * retmax
      val idsFuture = fetchData(pico, start, retmax, email)
      idsFuture.flatMap { newIds =>
        if (newIds.isEmpty || total >= retmax) {
          Future.successful(ids)
        } else {
          ids ++= newIds
          total += newIds.length
          page += 1
          println(s"Retrieved ${newIds.length} results on page $page")
          fetchDataPage()
        }
      }
    }

    fetchDataPage().map { _ =>
      println(s"Retrieved a total of $total results")
      ids
    }
  }*/

  private def buildQuery(pico: Pico): String = {
    val patientTerms = pico.problem.split("\\s+").map(_.toLowerCase)
    val interventionTerms = pico.intervention.split("\\s+").map(_.toLowerCase)
    val comparisonTerms = pico.comparison.get.split("\\s+").map(_.toLowerCase)
    val outcomeTerms = pico.outcome.split("\\s+").map(_.toLowerCase)

    val patientQuery = patientTerms.mkString(" OR ")
    val interventionQuery = interventionTerms.mkString(" OR ")
    val comparisonQuery = comparisonTerms.mkString(" OR ")
    val outcomeQuery = outcomeTerms.mkString(" OR ")

    val query = s"$patientQuery AND $interventionQuery AND $comparisonQuery AND $outcomeQuery"
    query
  }
}
