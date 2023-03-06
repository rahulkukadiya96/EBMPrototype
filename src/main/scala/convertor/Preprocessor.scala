package convertor

import edu.stanford.nlp.ling.CoreAnnotations.{LemmaAnnotation, PartOfSpeechAnnotation}
import edu.stanford.nlp.pipeline.{CoreDocument, StanfordCoreNLP}

import java.util.Properties
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
    val lemmas = relevantTokens.map(token => token.get(classOf[LemmaAnnotation]))
    lemmas.mkString(" ")
  }
}
