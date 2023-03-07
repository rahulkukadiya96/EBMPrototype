package generator

import classifier.SearchTerms
import dao.{DescriptorName, MeSHLoaderDao}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StaticMeSHSearch {
  def classifyTerms(terms: Seq[String], dao: MeSHLoaderDao): Future[SearchTerms] = {
    for {
      subjectHeadingList <- dao.getTerms(terms)
      (subjectHeading, keywords) <- classifyTerms(terms, subjectHeadingList)
    } yield {
//      println(s"Keyword List is $keywords")
      SearchTerms(keywords, subjectHeading)
    }
  }

  private def classifyTerms(terms: Seq[String], subjectHeadingList: Seq[DescriptorName]) = Future {
    val descriptorList = subjectHeadingList.map(_.name)
    terms.partition(text => descriptorList.contains(text.trim.toLowerCase))
  }
}
