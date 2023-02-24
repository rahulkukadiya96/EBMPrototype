package generator

import parser.CustomXMLParser

import scala.concurrent.Future
import scala.io.Source
import scala.util.Using
import scala.xml.Elem
import scala.concurrent.ExecutionContext.Implicits.global

object ExternalCallUtils {
  def callApi[A](url: String, processor: Elem => Seq[A]): Future[Seq[A]] = Future {
    println(s"URL for endpoint is $url")
    Using(Source.fromURL(url)) {
      data =>
        try {
          print(s"data is $data")
          val xml = CustomXMLParser.loadString(data.mkString)
          println(s"Generated xml is $xml")
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

  def consumeJsonAPI[A](url: String, processor: String => Seq[A]): Future[Seq[A]] = Future {
    println(s"URL for endpoint is $url")
    Using(Source.fromURL(url)) {
      data =>
        try {
          println(s"Generated xml is ${data.mkString}")
          processor(data.mkString)
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
}
