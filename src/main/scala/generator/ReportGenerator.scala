package generator

import com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.{PdfFont, PdfFontFactory}
import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter}
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.{Image, Paragraph}
import models.{Article, PatientSoap, Pico}
import schema.DBSchema.config

import java.io.FileOutputStream


object ReportGenerator {
  val BOLD = "Bold"
  private lazy val TEMP_OUTPUT_PATH = config.getString("temp_output_report_path")
  private lazy val REPORT_LOGO_PATH = config.getString("report_logo_path")

  private lazy val TITLE_FONT = {
    val boldFont: PdfFont = PdfFontFactory.createFont(HELVETICA_BOLD)
    boldFont
  }

  /*def getReport(): Pdf = {
    val templateStream = getClass.getResourceAsStream(TEMPLATE_FILE_PATH)
    val templateReader = new PdfReader(templateStream)
    val outputStream = new ByteArrayOutputStream()
    val pdfDoc = new PdfDocument(templateReader, new PdfWriter(outputStream))
    val form = PdfAcroForm.getAcroForm(pdfDoc, true)

    // Return the byte data of the generated PDF document as an array
    /*val pdfData = outputStream.toByteArray*/
    val pdfData = toByteArray(templateStream)
    pdfDoc.close()
    outputStream.close()
    templateReader.close()
    Pdf(pdfData)
  }*/

 /* def getReport(): Pdf = {
    val templateStream = getClass.getResourceAsStream(TEMPLATE_FILE_PATH)
    val templateReader = new PdfReader(templateStream)
    val outputStream = new ByteArrayOutputStream()
    val pdfDoc = new PdfDocument(templateReader, new PdfWriter(outputStream))
    val form = PdfAcroForm.getAcroForm(pdfDoc, true)

    // Return the byte data of the generated PDF document as an array
    /*val pdfData = outputStream.toByteArray*/
    val pdfData = toByteArray(templateStream)
    pdfDoc.close()
    outputStream.close()
    templateReader.close()

    val outputPath = "D:\\education_workspace\\Report.pdf";
    val newParagraph = "This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.This is a new paragraph that will be added to the PDF.";
    Pdf(pdfData)
  }*/

  def getReport(patientSoap: Option[PatientSoap], picoOption: Option[Pico], articles: Seq[Article]): String = {
    (patientSoap, picoOption) match {
      case (Some(soap), Some(pico)) => {
        val pdfDoc = new PdfDocument(new PdfWriter(new FileOutputStream(TEMP_OUTPUT_PATH)))
        val doc = new Document(pdfDoc)

        // Add logo image to the PDF
        addLogo(doc)

        // ADD SOAP
        feedSOAP(doc, soap)

        // ADD PICO
        feedPICO(doc, pico)

        doc.close()
        pdfDoc.close()
        "SUCCESS"
      }
      case (_, _) =>
        "No SOAP or PICO Found"
    }
  }

  private def addLogo(doc : Document): Unit = {
    val logo = new Image(ImageDataFactory.create(REPORT_LOGO_PATH))
    doc.add(logo)
  }

  private def feedSOAP(document: Document, soap : PatientSoap): Unit = {
    addTitleWithParagraph(document, "Subject", soap.subjectiveNodeData.toString)
    addTitleWithParagraph(document, "Object", soap.objective.toString)
    addTitleWithParagraph(document, "Assessment", soap.assessment.toString)
    addTitleWithParagraph(document, "Plan", soap.plan.toString)
  }

  private def feedPICO(document: Document, pico : Pico): Unit = {
    addTitleWithParagraph(document, "Patient/Problem", pico.problem)
    addTitleWithParagraph(document, "Intervention", pico.intervention)
    addTitleWithParagraph(document, "Comparison", pico.comparison.getOrElse(""))
    addTitleWithParagraph(document, "Outcome", pico.outcome)
    addTitleWithParagraph(document, "Time", pico.timePeriod.getOrElse(""))
  }

  private def addTitleWithParagraph(document: Document, titleText : String, para: String): Unit = {
    val title = new Paragraph(titleText)
    title.setFont(TITLE_FONT)
    document.add(title)

    val paragraph = new Paragraph(para)
    document.add(paragraph)
  }
}
