package generator

import parser.CustomXMLParser

import java.net.URLEncoder.encode
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Using
import scala.xml.Elem

object ExternalCallUtils {
  def callApi[A](url: String, processor: Elem => Seq[A]): Future[Seq[A]] = Future {
    Using(Source.fromURL(url)) {
      data =>
        try {
          val xml = CustomXMLParser.loadString(data.mkString)
          processor(xml)
        } catch {
          case e: Exception =>
            e.printStackTrace()
            Seq.empty
        }
    }.getOrElse {
      Seq.empty
    }
  }

  def extractIdFromXml(xml: Elem): Seq[String] = {
    val ids = (xml \\ "IdList" \\ "Id").map(_.text)
    ids
  }

  def urlEncode(query: String): String = {
    encode(query, "UTF-8")
  }
}
