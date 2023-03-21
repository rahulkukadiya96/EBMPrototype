package generator

import parser.CustomXMLParser

import java.net.URLEncoder.encode
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Using
import scala.xml.Elem

object ExternalCallUtils {
  def callApi[A](url: String, processor: Elem => Future[Seq[A]]): Future[Seq[A]] = {
    Using(Source.fromURL(url)) {
      data =>
        try {
          val xml = CustomXMLParser.loadString(data.mkString)
          println(s"Response XML is $xml")
          processor(xml)
        } catch {
          case e: Exception =>
            e.printStackTrace()
            Future {
              Seq.empty
            }
        }
    }.getOrElse {
      Future {
        Seq.empty
      }
    }
  }

  def extractIdFromXml(xml: Elem): Future[Seq[String]] = Future {
    val ids = (xml \\ "IdList" \\ "Id").map(_.text)
    ids
  }

  def urlEncode(query: String): String = {
    encode(query, "UTF-8")
  }

  def extractCountFromXml(xml: Elem): Future[Seq[Int]] = Future {
    val totalCount = (xml \ "Count").text.toIntOption
    totalCount match {
      case Some(value) => Seq(value)
      case None => Seq.empty
    }
  }
}
