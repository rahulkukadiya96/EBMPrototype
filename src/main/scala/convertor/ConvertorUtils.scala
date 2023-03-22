package convertor

import convertor.Preprocessor.preprocessText
import models.{PatientSoap, Pico}

import java.time.LocalDateTime
import scala.Option.empty

object ConvertorUtils {
  def toCleanText[A](a: A): String = preprocessText(a.toString)

  def toCleanText[A](a: List[A]): String = preprocessText(a.mkString(" "))

  def transformList(comparison: Option[String])(listData: Seq[PatientSoap]): Seq[Pico] = Seq.empty

/*
  def generatePico(comparison: Option[String] = empty)(note: PatientSoap): Pico = Pico(0, toCleanText(note.subjectiveNodeData), toCleanText(note.plan), comparison, toCleanText(List(note.assessment, note.objective)), empty, Option(LocalDateTime.now()))
*/
}
