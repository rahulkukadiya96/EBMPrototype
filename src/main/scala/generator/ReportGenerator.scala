package generator

import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfFormField
import com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.{PdfFont, PdfFontFactory}
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter}
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.{Image, LineSeparator, Paragraph}
import com.itextpdf.layout.properties.HorizontalAlignment
import models.{Article, PatientSoap, Pico}
import schema.DBSchema.config

import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.UUID
import java.util.UUID.randomUUID


object ReportGenerator {
  val BOLD = "Bold"
  private lazy val TEMP_OUTPUT_PATH = config.getString("temp_output_report_path")
  private lazy val REPORT_LOGO_PATH = config.getString("report_logo_path")

  private lazy val BOLD_FONT: PdfFont = PdfFontFactory.createFont(HELVETICA_BOLD)

  private lazy val TITLE_FONT = BOLD_FONT



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
        val filePath = Paths.get(TEMP_OUTPUT_PATH, s"report_${randomUUID()}.pdf").toString
        val fileOutputStream = new FileOutputStream(filePath)
        val pdfWriter = new PdfWriter(fileOutputStream)
        val pdfDoc = new PdfDocument(pdfWriter)
        val doc = new Document(pdfDoc)

        // Add logo image to the PDF
        addLogo(doc)

        // ADD SOAP
        feedSOAP(doc, soap)

        // ADD PICO
        feedPICO(doc, pico)

        // ADD Articles
        feedArticleSummary(doc, articles)
        // ADD Physician Note field
        /*addPhysicianNote(pdfDoc)*/

        doc.close()
        pdfDoc.close()
        pdfWriter.close()
        fileOutputStream.close()

        "SUCCESS"
      }
      case (_, _) =>
        "No SOAP or PICO Found"
    }
  }

  private def addLogo(doc : Document): Unit = {
    val logo = new Image(ImageDataFactory.create(REPORT_LOGO_PATH))
    logo.setHorizontalAlignment(HorizontalAlignment.CENTER)
    logo.setHeight(150f)
    doc.add(logo)

    addLineSeparator(doc)
  }

  private def addLineSeparator(doc : Document) : Unit = {
    val line = new LineSeparator(new SolidLine())
    doc.add(line)
  }

  private def feedSOAP(document: Document, soap : PatientSoap): Unit = {
    addHeading(document, "SOAP Note")
    addTitleWithParagraph(document, "Subject", soap.subjectiveNodeData.toString)
    addTitleWithParagraph(document, "Object", soap.objective.toString)
    addTitleWithParagraph(document, "Assessment", soap.assessment.toString)
    addTitleWithParagraph(document, "Plan", soap.plan.toString)
  }

  private def feedPICO(document: Document, pico : Pico): Unit = {
    addHeading(document, "PICO Note")
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

    addParagraph(document, para)
  }

  private def addParagraph(document : Document, para:String): Unit = {
    val paragraph = new Paragraph(para)
    document.add(paragraph)
  }

  private def addHeading(document: Document, headingText: String ) {
    val heading = new Paragraph(headingText)
    heading.setFont(BOLD_FONT)
    heading.setFontSize(20f)
    document.add(heading)
  }

  private def feedArticleSummary(document : Document, articles: Seq[Article]): Unit = {
    addHeading(document, "Articles")
    articles.filter(_.summary.nonEmpty).foreach(article => addParagraph(document, article.summary.get))
  }

  private def addPhysicianNote(pdf : PdfDocument): Unit = {
    val rect = new Rectangle(100f, 600f, 1000f, 200f)
    val acroForm = PdfAcroForm.getAcroForm(pdf, true)
    val field = PdfFormField.createText(pdf, rect, "Notes")
    acroForm.addField(field)
  }
}
