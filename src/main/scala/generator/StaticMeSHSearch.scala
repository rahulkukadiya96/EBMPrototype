package generator

import classifier.SearchTerms
import dao.MeSHLoaderDao
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StaticMeSHSearch {
  def classifyTerms(terms: Seq[String], dao: MeSHLoaderDao): Future[SearchTerms] = {
    for {
      subjectHeadingList <- dao.getTerms(terms)
      (subjectHeading, keywords) <- classifyTerms(terms, subjectHeadingList)
    } yield {
      SearchTerms(keywords, subjectHeading)
    }
  }

  private def classifyTerms(terms: Seq[String], subjectHeadingList: Seq[String]) = Future {
    terms.partition(subjectHeadingList.contains(_))
  }
}
