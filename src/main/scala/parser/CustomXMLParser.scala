package parser

import scala.xml.{Elem, SAXParser}
import scala.xml.factory.XMLLoader

object CustomXMLParser extends XMLLoader[Elem] {
  override def parser: SAXParser = {
    val f = javax.xml.parsers.SAXParserFactory.newInstance()
    f.setNamespaceAware(false)
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    f.newSAXParser()
  }
}
