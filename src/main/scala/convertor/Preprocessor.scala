package convertor

import edu.stanford.nlp.ling.CoreAnnotations.{LemmaAnnotation, PartOfSpeechAnnotation}
import edu.stanford.nlp.pipeline.{CoreDocument, StanfordCoreNLP}
import generator.RestExternalCallUtils.summarizeAbstract
import models.{Article, ArticleSummaryRequest, ArticleSummaryRequestData, ArticleSummaryResponse, RougeScore, RougeScores, SummaryResponse}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json.{stringify, toJson}
import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import schema.DBSchema.config

import java.time.Duration
import java.util.Properties
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

object Preprocessor {

  def preprocessText(text: String): String = {
    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma")
    val pipeline = new StanfordCoreNLP(props)
    val document = new CoreDocument(text)
    pipeline.annotate(document)
    val tokens = document.tokens().asScala
    val relevantTokens = tokens.filter(token => token.get(classOf[PartOfSpeechAnnotation]).startsWith("N") || token.get(classOf[PartOfSpeechAnnotation]).startsWith("V"))
    val lemmas = relevantTokens.map(token => token.get(classOf[LemmaAnnotation])).toSeq
    lemmas.distinct.mkString(" ")
  }

  def generateNGram(string: Seq[String], nGram:Int) : Seq[String] = string.sliding(nGram).toList.map(_.mkString(" "))

  def getFormattedWords(string: Seq[String]) : Seq[String] = generateNGram(string, 1) ++ generateNGram(string, 2) ++ generateNGram(string, 3)

  def getKeywords(string: String) : Seq[String] = {
    val list = getFormattedWords(preprocessText(string).split(" ").toSeq)
    println(s"List is $list")
    list
  }

  def generateAbstractUsingBart(articles: Seq[Article]): Future[Map[Long, String]] = {
    val articleData = articles.filter(article => Option(article).nonEmpty).map(article => (article.id.getOrElse(0), article.abstractText))
    articleData.isEmpty match {
      case true => Future {
        Map.empty[Long, String]
      }
      case false =>
        val url = config.getString("bart_url")
        val headers = Map("Content-Type" -> "application/json", "Authorization" -> s"Bearer ${config.getString("bart_token")}")
        val max_length = config.getInt("max_token_len")
        val min_length = config.getInt("min_token_len")
        val readTimeouts = Duration.ofMinutes(config.getInt("bart_read_timeout")).toMillis.toInt
        val connectionTimeouts = Duration.ofMinutes(config.getInt("bart_connection_timeout")).toMillis.toInt
        val parameters = s""" {"max_length": $max_length, "min_length": $min_length }"""

        val chunks = articleData.grouped(config.getInt("abstract_batch_size")).toList
        val futures = chunks.map { chunk =>
          val text = toJson(chunk.map(_._2))
          val payload = s""" {"inputs": $text, "parameters": $parameters } """
          Future {
            val response = summarizeAbstract(url, headers, payload, readTimeouts, connectionTimeouts)
            val summary = Json.parse(response.text).as[JsArray].value.map(_.as[JsObject].value("summary_text").as[String]).toList
            chunk.zipWithIndex.map {
              case ((id : Long, _), index:Int) => {
                val summaryData = summary(index)
                id -> summaryData
              }
            }.toMap
          }
        }
        Future.sequence(futures).map(_.flatten.toMap)
    }
  }

  implicit val articleWrites: Writes[ArticleSummaryRequest] = Json.writes[ArticleSummaryRequest]
  def generateAbstractUsingBioBart(articles: Seq[Article]): Future[Map[Long, ArticleSummaryResponse]] = {
    val articleData = articles.filter(article => Option(article).nonEmpty && article.summary.nonEmpty).map(article => ArticleSummaryRequest(article.id.getOrElse(0), article.abstractText, 100))
    articleData.nonEmpty match {
      case false => Future {
        Map.empty
      }
      case true =>
        val url = config.getString("bio_bart_url")
        val headers = Map("Content-Type" -> "application/json")
        val readTimeouts = Duration.ofMinutes(config.getInt("bart_read_timeout")).toMillis.toInt
        val connectionTimeouts = Duration.ofMinutes(config.getInt("bart_connection_timeout")).toMillis.toInt
        val batchSize = config.getInt("abstract_batch_size")
        val chunks = articleData.grouped(batchSize).toSeq

        val futures = chunks.map { chunk =>
          val payload =  stringify(toJson(ArticleSummaryRequestData(articles = chunk)))

          /*val json = Json.toJson(chunk.map(a => Json.obj(
            "text" -> a.text,
            "maxlength" -> a.maxLength,
            "id" -> a.id
          )))
          val payload = Json.stringify(Json.obj("articles" -> json))
          */



          Future {
            val response = summarizeAbstract(url,  headers, payload, readTimeouts, connectionTimeouts)
            val summaryResponse: SummaryResponse = Json.parse(response.text).as[SummaryResponse]
            summaryResponse.success match {
              case true => summaryResponse.data.map(summaryData => (summaryData.id, summaryData)).toMap
              case false => Map.empty
            }
          }
        }
          Future.sequence(futures).map(_.flatten.toMap)
    }
  }

  def getAvgScore(scores: Seq[String]): Future[String] = Future {
    val scoreList = scores.map(score => score.replaceAll( "'", "\"")).map(score => Json.parse(score).as[RougeScores])
    val averageRougeScores: RougeScores = scoreList.foldLeft(RougeScores(RougeScore(0, 0, 0), RougeScore(0, 0, 0), RougeScore(0, 0, 0))) { (acc, curr) =>
      RougeScores(
        RougeScore((acc.rouge1.r + curr.rouge1.r) / 2, (acc.rouge1.p + curr.rouge1.p) / 2, (acc.rouge1.f + curr.rouge1.f) / 2),
        RougeScore((acc.rouge2.r + curr.rouge2.r) / 2, (acc.rouge2.p + curr.rouge2.p) / 2, (acc.rouge2.f + curr.rouge2.f) / 2),
        RougeScore((acc.rougel.r + curr.rougel.r) / 2, (acc.rougel.p + curr.rougel.p) / 2, (acc.rougel.f + curr.rougel.f) / 2)
      )
    }
    Json.toJson(averageRougeScores).toString()
  }

  /*def generateAbstract(articles : Seq[Article]): Future[Map[String, String]] = Future {
    val articleData = articles.filter(article => Option(article).nonEmpty).map(article => (article.id.getOrElse(0), article.abstractText))
    articleData.isEmpty match {
      case true => Map.empty[Long, String]
      case false =>
        val url = config.getString("bart_url")
        val headers = Map("Content-Type" -> "application/json", "Authorization" -> s"Bearer ${config.getString("bart_token")}")
        val max_length = config.getInt("max_token_len")
        val min_length = config.getInt("min_token_len")
        val readTimeouts = Duration.ofMinutes(config.getInt("bart_read_timeout")).toMillis.toInt
        val connectionTimeouts = Duration.ofMinutes(config.getInt("bart_connection_timeout")).toMillis.toInt
        val parameters = s""" {"max_length": $max_length, "min_length": $min_length }"""

        val chunks = articleData.grouped(config.getInt("abstract_batch_size")).toList
        /*val futures = chunks.map { chunk =>
          val text = Json.toJson(chunk.map(_._2))
          val payload = s""" {"inputs": $text, "parameters": $parameters } """
          Future {
            summarizeAbstract(url, headers, payload, readTimeouts, connectionTimeouts)
          }
        }*/
        val futures = chunks.map { chunk =>
          val text = Json.toJson(chunk.map(_._2))
          val payload = s""" {"inputs": $text, "parameters": $parameters } """
          Future {
            chunk.map { case (id, _) =>
              val response = summarizeAbstract(url, headers, payload, readTimeouts, connectionTimeouts)
              val summary = Json.parse(response.text).as[JsObject].value("summary_text").as[String]
              id -> summary
            }.toMap
          }
        }
        Future.sequence(futures).map(_.flatten.toMap)
        /*Await.result(Future.sequence(futures), 5.minutes)*/
        /*val text = Json.toJson(articleData.map(_._2))
        val payload = s""" {"inputs": $text, "parameters": $parameters } """
        summarizeAbstract(url, headers, payload, readTimeouts, connectionTimeouts)*/
        /*BaseResponse(statusCode = 200, message = Some("Success"))*/
    }
  }*/
}
