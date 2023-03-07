package generator

import dao.MeSHLoaderDao
import generator.ExternalCallUtils.{callApi, extractIdFromXml, urlEncode}
import generator.MeSHSearch.searchSubjectHeading
import models.{Pico, Response}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PubMedSearch {
  val BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"
  val OR = " OR "
  val AND = " AND "
  val PUBMED_DB_NAME = "pubmed"

  def fetchDataWithStaticClassifier(picoD: Option[Pico], dao: MeSHLoaderDao, retMax: Int = 10): Future[Response] = {
    picoD match {
      case Some(pico) =>
        for {
          problem_terms <- StaticMeSHSearch.classifyTerms(pico.problem.split(" "), dao)
          problem_search_terms <- subjectHeadingJoiner(problem_terms.subject_headings)
//          problem_search_terms <- searchSubjectHeading(problem_terms.subject_headings, subjectHeadingJoiner)

          outcome_terms <- StaticMeSHSearch.classifyTerms(pico.outcome.split(" "), dao)
          outcome_search_terms <- subjectHeadingJoiner(outcome_terms.subject_headings)
//          outcome_search_terms <- searchSubjectHeading(outcome_terms.subject_headings, subjectHeadingJoiner)

          intervention_terms <- StaticMeSHSearch.classifyTerms(pico.intervention.split(" "), dao)
          intervention_search_terms <- subjectHeadingJoiner(intervention_terms.subject_headings)
//          intervention_search_terms <- searchSubjectHeading(intervention_terms.subject_headings, subjectHeadingJoiner)

          intervention_terms <- StaticMeSHSearch.classifyTerms(pico.comparison.get.split(" "), dao)
          comparision_search_terms <- subjectHeadingJoiner(intervention_terms.subject_headings)
//          comparision_search_terms <- searchSubjectHeading(intervention_terms.subject_headings, subjectHeadingJoiner)

          query <- buildQuery(Option(problem_search_terms), Option(outcome_search_terms), Option(intervention_search_terms), Option(comparision_search_terms))
          response <- executeQuery(query, retMax)
        } yield {
          response
        }
      case None =>
        Future {
          Response("No data found", 200)
        }
    }
  }

  def executeQuery(query: Option[String], retMax: Int) = {
    query match {
      case Some(queryStr) =>
        for {
          url <- buildUrl(queryStr, retMax)
          ids <- callApi(url, extractIdFromXml)
          response <- buildResponseObject(ids)
        } yield {
          response
        }
      case None => Future {
        Response("No data found", 200)
      }
    }
  }

  def buildResponseObject(ids: Seq[String]): Future[Response] = Future {
    Response(ids.mkString(","), 200)
  }

  def subjectHeadingJoiner(seq: Seq[String]): Future[String] = Future {
    joiner(seq, OR)
  }

  def joiner[A](seq: Seq[A], del: String): String = seq.mkString(del)

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

  private def buildQuery(patientQuery: Option[String], interventionQuery: Option[String], outcomeQuery: Option[String], comparisonQuery: Option[String]): Future[Option[String]] = Future {
    val queryList = List(patientQuery, interventionQuery, outcomeQuery, comparisonQuery).filter(_.isDefined).map(_.get).filter(_.nonEmpty)
    queryList.isEmpty match {
      case true => Option.empty
      case false => Option(queryList.map("(" + _ + ")").mkString("(", AND, ")"))
    }
  }

  private def buildUrl(query: String, retMax: Int): Future[String] = Future {
    s"$BASE_URL/esearch.fcgi?db=$PUBMED_DB_NAME&term=${urlEncode(query)}&retmax=$retMax"
  }
}