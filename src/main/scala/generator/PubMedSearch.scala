package generator

import convertor.Preprocessor.getKeywords
import dao.{AppDAO, MeSHLoaderDao}
import generator.ExternalCallUtils.{callApi, extractCountFromXml, extractIdFromXml, urlEncode}
import generator.StaticMeSHSearch.classifyTerms
import models.{AbstractComponent, Article, Pico, Response}
import schema.DBSchema.config

import java.lang.Math.min
import scala.Option.empty
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.{Elem, Node, NodeSeq, Utility, XML}

object PubMedSearch {
  val BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"
  val OR = " OR "
  val AND = " AND "
  val PUBMED_DB_NAME = "pubmed"
  val API_KEY: String = config.getString("ncbi_api_key")
  val API_EMAIL: String = config.getString("ncbi_email")

  def buildQueryWithStaticClassifier(picoD: Option[Pico], dao: MeSHLoaderDao, appDAO: AppDAO): Future[Option[String]] = {
    picoD match {
      case Some(pico) =>
        pico.searchQuery match {
          case Some(_) => Future {
            pico.searchQuery
          }
          case None =>
            for {
              problem_terms <- classifyTerms(getKeywords(pico.problem), dao)
              problem_search_terms <- subjectHeadingJoiner(problem_terms.subject_headings)

              outcome_terms <- classifyTerms(getKeywords(pico.outcome), dao)
              outcome_search_terms <- subjectHeadingJoiner(outcome_terms.subject_headings)

              intervention_terms <- classifyTerms(getKeywords(pico.intervention), dao)
              intervention_search_terms <- subjectHeadingJoiner(intervention_terms.subject_headings)

              comp_terms <- classifyTerms(getKeywords(pico.comparison.getOrElse("")), dao)
              comparision_search_terms <- subjectHeadingJoiner(comp_terms.subject_headings)

              query <- buildQuery(Option(problem_search_terms), Option(outcome_search_terms), Option(intervention_search_terms), Option(comparision_search_terms))
            } yield {
              query
            }
        }
      case None =>
        Future {
          None
          /*Response(empty, 200, Option("No data found"), empty)*/
        }
    }
  }

  def executeQuery(query: Option[String], retMax: Int): Future[Response] = {
    query match {
      case Some(queryStr) =>
        for {
          url <- buildUrl(queryStr, retMax)
          ids <- callApi(url, extractIdFromXml)
          response <- buildResponseObject(ids, queryStr)
        } yield {
          response
        }
      case None => Future {
        Response(empty, 200, Option("No data found"), empty)
      }
    }
  }

  def buildResponseObject(ids: Seq[String], query : String): Future[Response] = Future {
    Response(Option(ids.flatMap(_.toIntOption)), 200, Option("Success"), Option(query))
  }

  def subjectHeadingJoiner(seq: Seq[String]): Future[String] = Future {
    joiner(seq, OR)
  }

  def joiner[A](seq: Seq[A], del: String): String = seq.mkString(del)

  private def buildQuery(patientQuery: Option[String], interventionQuery: Option[String], outcomeQuery: Option[String], comparisonQuery: Option[String]): Future[Option[String]] = Future {
    val queryList = List(patientQuery, interventionQuery, outcomeQuery, comparisonQuery).filter(_.isDefined).map(_.get).filter(_.nonEmpty)
    queryList.isEmpty match {
      case true => empty
      case false => Option(queryList.map("(" + _ + ")").mkString("(", AND, ")"))
    }
  }

  private def buildUrl(query: String, retMax: Int): Future[String] = Future {
    s"$BASE_URL/esearch.fcgi?db=$PUBMED_DB_NAME&term=${urlEncode(query)}&retmax=$retMax"
  }

  def executeQuery(pico : Option[Pico], query: Option[String], appDao : AppDAO, limit : Int): Future[Response] = {
    val pageSize = 5
    query match {
      case Some(queryStr) =>
        pico match {
          case Some(picoVal) =>
            val encodedQuery= urlEncode(queryStr)
            for {
              recordCount <- fetchCounts(encodedQuery)
              totalPages <- totalPages(min(recordCount, limit), pageSize)
              additionParam <- fetchAdditionalParams(encodedQuery, pageSize)
            } yield {
              // Remove existing articles
              appDao.removeAllArticles(picoVal.id)
              /*val articles = (1 to totalPages).map(_ => fetchRecord(encodedQuery, picoVal.id, appDao, _, pageSize))*/
              for (pageNo <- 1 to totalPages) {
                fetchRecord(additionParam, picoVal.id, appDao, pageNo, pageSize)
              }
              Response(empty, 200, Option(s"Total articles saved are ${min(recordCount, limit)}"), empty)
            }
          case None => Future {
            Response(empty, 200, Option("No pico data found"), empty)
          }
        }
      case None => Future {
        Response(empty, 200, Option("No data found"), empty)
      }
    }
  }
  private def fetchCounts(queryStr : String) : Future[Int] = {
    val countUrl = s"$BASE_URL/esearch.fcgi?db=$PUBMED_DB_NAME&rettype=count&term=$queryStr&api_key=$API_KEY&email=$API_EMAIL"
    for {
      count <- callApi(countUrl, extractCountFromXml)
    } yield {
      count.headOption.getOrElse(0)
    }
  }

  private def fetchAdditionalParams(queryStr: String, retMax: Int) = {
    val extractParams = s"$BASE_URL/esearch.fcgi?db=$PUBMED_DB_NAME&term=$queryStr&api_key=$API_KEY&email=$API_EMAIL&usehistory=y&retmax=$retMax"
    for {
      additionalParams <- callApi(extractParams, extractAdditionalParams)
    } yield {
      additionalParams.headOption.getOrElse(Map.empty)
    }
  }

  def totalPages(totalCount: Int, pageSize: Int): Future[Int] = Future {
    math.ceil(totalCount.toDouble / pageSize).toInt
  }
  private def fetchRecord(additionParam : Map[String, String], picoId : Int, dao : AppDAO, pageNum : Int, pageSize : Int = 20) = {
    val webEnv = additionParam.getOrElse("webEnv", "")
    val queryKey = additionParam.getOrElse("queryKey", "")
    val startIndex = (pageNum - 1) * pageSize
    val fetchUrl = s"$BASE_URL/efetch.fcgi?db=$PUBMED_DB_NAME&retmode=xml&rettype=abstract&retstart=$startIndex&retmax=$pageSize&query_key=$queryKey&api_key=$API_KEY&email=$API_EMAIL&WebEnv=$webEnv"
    for {
      articles <- callApi(fetchUrl, extractArticle)
    } yield dao.saveArticles(picoId, articles)
  }


  private def extractArticle(xml: Elem): Future[Seq[Article]] = Future {
    val articles = xml \\ "PubmedArticle"
    articles.map(toArticle)
  }

  def extractAdditionalParams(xml: Elem): Future[Seq[Map[String, String]]] = Future {
    val webenv = (xml \ "WebEnv").text
    val querykey = (xml \ "QueryKey").text
    Seq(Map("webEnv" -> webenv, "queryKey" -> querykey))
  }

  private def toArticle(article : Node) = {
    Article(
      id = None,
      title = (article \ "MedlineCitation" \ "Article" \ "ArticleTitle").text,
      authors = (article \ "MedlineCitation" \ "Article" \ "AuthorList" \ "Author" \ "LastName").map(_.text).mkString(", "),
      journal = (article \ "MedlineCitation" \ "Article" \ "Journal" \ "Title").text,
      pubDate = (article \ "MedlineCitation" \ "Article" \ "Journal" \ "JournalIssue" \ "PubDate" \ "Year").text,
      abstractText = (article \ "MedlineCitation" \ "Article" \ "Abstract").text,
      summary = None,
      abstractComponent = toArticleAbstract(article)
    )
  }

  /*private def toArticleAbstract(abstractComponent: NodeSeq) = {
    /*AbstractComponent(
      background = extractAbstractText(abstractText, "BACKGROUND"),
      objectives = extractAbstractText(abstractText, "OBJECTIVES"),
      methods = extractAbstractText(abstractText, "METHODS"),
      result = extractAbstractText(abstractText, "RESULTS"),
      conclusion = extractAbstractText(abstractText, "CONCLUSIONS"),
    )*/

    AbstractComponent(
      background = Some(abstractComponent.filter(n => (n \@ "Label") == "BACKGROUND").text),
      objectives = Some(abstractComponent.filter(n => (n \@ "Label") == "OBJECTIVES").text),
      methods = Some(abstractComponent.filter(n => (n \@ "Label") == "METHODS").text),
      result = Some(abstractComponent.filter(n => (n \@ "Label") == "RESULTS").text),
      conclusion = Some(abstractComponent.filter(n => (n \@ "Label") == "CONCLUSIONS").text),
    )
  }*/

  private def toArticleAbstract(article: Node): Option[AbstractComponent] = {
    val abstractTexts = (article \ "MedlineCitation" \ "Article" \"Abstract" \ "AbstractText")
      .map(e => (e \@ "Label", e.text))
      .toMap

    abstractTexts.foreach { case (label, text) =>
      println(s"$label: $text")
    }
    Some(AbstractComponent(
      background = abstractTexts.get("BACKGROUND"),
      objectives = abstractTexts.get("OBJECTIVES"),
      methods = abstractTexts.get("METHODS"),
      result = abstractTexts.get("RESULTS"),
      conclusion = abstractTexts.get("CONCLUSION"),
    ))
  }

  /*private def toArticleAbstract(abstractComponents: Node): Option[AbstractComponent] = {
    val xmlString = Utility.serialize(abstractComponents)
    println(xmlString)
    val abstractTexts = abstractComponents \\ "Abstract" \\ "AbstractText"
    val abstractMap = abstractTexts.map(a => (a \@ "Label", a.text)).toMap
    print(s"abstractMap is ${abstractMap}")
    abstractComponents.headOption match {
      case Some(abstractComponent) =>
        print(s"Head data is ${abstractComponent.text}")
        val background = (abstractComponents \\ "AbstractText").filter(node => (node \@ "Label") == "BACKGROUND").text.trim
        print(s"background data is ${background}")

        Some(AbstractComponent(
          background = Some((abstractComponent).filter(n => (n \@ "Label") == "BACKGROUND").text),
          objectives = Some((abstractComponent).filter(n => (n \@ "Label") == "OBJECTIVES").text),
          methods = Some((abstractComponent).filter(n => (n \@ "Label") == "METHODS").text),
          result = Some((abstractComponent).filter(n => (n \@ "Label") == "RESULTS").text),
          conclusion = Some((abstractComponent).filter(n => (n \@ "Label") == "CONCLUSION").text),
        ))
      case _ => None
    }
  }*/
  private def extractAbstractText(abstractComponent: NodeSeq, label: String): Option[String] = {
    val abstractText = abstractComponent \ "AbstractText"
    abstractText.find(_.attribute("Label").exists(_.text == label)).map(_.text.trim)
  }
}