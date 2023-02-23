package generator

import java.net.URLEncoder.encode
import scala.io.Source
import scala.util.Using
import scala.xml.XML

object PubMedSearch {
  def fetchData(p: String, i: String, c: String, o: String, retmax: Int, email: String): Seq[String] = {
    val baseUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/"
    val query = s"$p+AND+$i+AND+$c+AND+$o"
    val url = baseUrl + s"esearch.fcgi?db=pubmed&retmax=$retmax&term=${encode(query, "UTF-8")}&email=$email"
    Using(Source.fromURL(url)) {
      data =>
        val xml = XML.loadString(data.mkString)
        if ((xml \\ "ErrorList" \\ "Error").nonEmpty) {
          throw new RuntimeException(s"PubMed API error: ${(xml \\ "ErrorList" \\ "Error" \ "Message").text}")
        } else {
          (xml \\ "IdList" \\ "Id").map(_.text)
        }
    }.getOrElse {
      Seq.empty
    }
  }

  def searchAll(p: String, i: String, c: String, o: String, email: String): Seq[String] = {
    val retmax = 10000
    var ids = Seq.empty[String]
    var page = 0
    var total = 0
    val newIds = fetchData(p, i, c, o, retmax, email)
    do {
      ids ++= newIds
      total += newIds.length
      page += 1
      println(s"Retrieved ${newIds.length} results on page $page")
    } while (newIds.nonEmpty && total < retmax)
    println(s"Retrieved a total of $total results")
    ids
  }
}
