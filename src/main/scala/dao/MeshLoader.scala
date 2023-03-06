package dao

import org.neo4j.driver.v1.{Driver, Record}
import parser.CustomXMLParser

import scala.io.Source
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters
import scala.util.Using

case class DescriptorUI(ui: String)

case class DescriptorName(name: String)

case class DescriptorTreeNumber(treeNumber: String)

case class DescriptorRecord(ui: DescriptorUI, name: DescriptorName, treeNumber: Seq[DescriptorTreeNumber])

class MeSHLoaderDao(connection: Driver) {

  def getTerms(terms: Seq[String]) = {
    val queryString = s"OPTIONAL MATCH (d : Descriptor) WHERE d.name IN [${terms.map(s => s"'$s'").map(_.trim.toLowerCase).mkString(",")}] RETURN d.name as name"
    getData(queryString, readDescriptorName)
  }

  private def readDescriptorName(record: Record) = {
    DescriptorName(
      name = record.get("name").asString()
    )
  }


  def loadDictionary(path: String): Boolean = {
    Using(Source.fromFile(path)) {
      dictionary =>
        try {
          val xml = CustomXMLParser.loadString(dictionary.getLines.mkString)
          val descriptorRecords = (xml \\ "DescriptorRecord").map { descriptorRecord =>
            val ui = DescriptorUI((descriptorRecord \ "DescriptorUI").text.trim.toLowerCase)
            val name = DescriptorName((descriptorRecord \ "DescriptorName").text.trim.toLowerCase)
            val treeNumbers = (descriptorRecord \ "TreeNumberList" \ "TreeNumber").map { treeNumber =>
              DescriptorTreeNumber(treeNumber.text.trim.toLowerCase)
            }
            DescriptorRecord(ui, name, treeNumbers)
          }

          val session = connection.session()
          session.run("MATCH (d:Descriptor) DETACH DELETE d")
          // Create the descriptor nodes and tree number relationships
          session.run("CREATE CONSTRAINT ON (d:Descriptor) ASSERT d.ui IS UNIQUE")
          session.run("CREATE INDEX ON :Descriptor(name)")

          for (descriptorRecord <- descriptorRecords) {
            session.run(s"CREATE (d:Descriptor {ui: '${descriptorRecord.ui.ui}', name: '${descriptorRecord.name.name}'})")

            /*session.run(
              """CREATE (d:Descriptor {ui: $ui, name: $name})
                       FOREACH (treeNumber IN $treeNumbers |
                         CREATE (d)-[:HAS_TREE_NUMBER]->(:TreeNumber {value: treeNumber.treeNumber})
                       )""",
              mapAsJavaMap(Map(
                "ui" -> descriptorRecord.ui.ui,
                "name" -> descriptorRecord.name.name,
                "treeNumbers" -> descriptorRecord.treeNumber
              ))
            )*/
          }

          session.close()
          true
        } catch {
          case e: Exception =>
            e.printStackTrace()
            false
        }
    }.getOrElse {
      false
    }
  }

  private def getData[T](query: String, reader: Record => T) = {
    val session = connection.session()
    val queryCompletion = session
      .runAsync(query)
      .thenCompose[java.util.List[T]](c => c.listAsync[T](record => reader(record)))
      .thenApply[Seq[T]] {
        _.asScala.toSeq
      }
      .whenComplete((_, _) => session.closeAsync())

    FutureConverters.CompletionStageOps(queryCompletion).asScala
  }
}
