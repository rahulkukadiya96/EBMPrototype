package classifier

import generator.ExternalCallUtils.callApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.xml.Elem

case class MeSHResult(term: String, isMeSH: Boolean, treeNumbers: Seq[String], definition: String)

case class SearchTerms(keywords: Seq[String], subject_headings: Seq[String])

object MeSHClassifier {
  private val baseUrl = "https://id.nlm.nih.gov/mesh"
  private val searchUrl = baseUrl + "/lookup/details?descriptor="
  private val pageSize = 100

  /**
   * Classifies the given terms as MeSH or non-MeSH terms.
   * Returns a sequence of MeSHResult objects containing the term, whether it is a MeSH term,
   * its tree numbers (if it is a MeSH term), and its definition (if available).
   */
  def classifyTerms(terms: Seq[String]): Future[Seq[SearchTerms]] = Future {
    val batches = terms.grouped(pageSize)
    val futures = batches.map(batch => classifyBatch(batch))
    val results = Await.result(Future.sequence(futures), 30.seconds)
    results.toSeq
  }

  private def classifyBatch(batch: Seq[String]): Future[SearchTerms] = {
    val queries = batch.map(term => urlEncode(term)).mkString(",")
    val finalUrl = searchUrl + queries
    callApi(finalUrl, extractMeshResultFromJson).flatMap {
      processMeshResult
    }
  }

  def extractMeshResultFromJson(xml: Elem): Seq[MeSHResult] = {
    (xml \\ "DescriptorRecord").map { descriptor =>
      val term = (descriptor \ "DescriptorName" \ "String").text
      val isMeSH = (descriptor \ "@DescriptorClass").text == "1"
      val treeNumbers = (descriptor \ "TreeNumberList" \ "TreeNumber").map(_.text)
      val definition = (descriptor \ "ScopeNote" \ "String").headOption.map(_.text).getOrElse("")
      MeSHResult(term, isMeSH, treeNumbers, definition)
    }
  }

  def processMeshResult(result: Seq[MeSHResult]): Future[SearchTerms] = Future {
    val (headings, keywords) = result.partition(_.isMeSH)
    SearchTerms(headings.map(_.term), keywords.map(_.term))
  }

  private def urlEncode(term: String): String = {
    java.net.URLEncoder.encode(term, "UTF-8")
  }
}