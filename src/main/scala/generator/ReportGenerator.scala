package generator

import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfFormField
import com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants.BLUE
import com.itextpdf.kernel.colors.{ColorConstants, DeviceRgb}
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.annot.PdfAnnotation
import com.itextpdf.kernel.pdf.annot.PdfAnnotation.STYLE_BEVELED
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.kernel.pdf.{PdfArray, PdfDictionary, PdfDocument, PdfName, PdfWriter}
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.{Image, LineSeparator, Paragraph}
import com.itextpdf.layout.properties.HorizontalAlignment
import models.{Article, Patient, PatientSoap, Pico}
import schema.DBSchema.config

import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.UUID.randomUUID


object ReportGenerator {
  private lazy val TEMP_OUTPUT_PATH = config.getString("temp_output_report_path")
  private lazy val REPORT_LOGO_PATH = config.getString("report_logo_path")



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

  def getReport(patientOp : Option[Patient], patientSoap: Option[PatientSoap], picoOption: Option[Pico], articles: Seq[Article]): String = {
    (patientOp, patientSoap, picoOption) match {
      case (Some(patient), Some(soap), Some(pico)) => {
        val filePath = Paths.get(TEMP_OUTPUT_PATH, s"report_${randomUUID()}.pdf").toString
        val fileOutputStream = new FileOutputStream(filePath)
        val pdfWriter = new PdfWriter(fileOutputStream)
        val pdfDoc = new PdfDocument(pdfWriter)
        val doc = new Document(pdfDoc)

        // Add logo image to the PDF
        addLogo(doc)

        // Add patient basic details
        feedPatientDetails(doc, patient)

        // ADD SOAP
        feedSOAP(doc, soap)

        // ADD PICO
        feedPICO(doc, pico)

        // ADD Articles
        feedArticleSummary(doc, articles)

        // ADD Physician Note field
        addHeading(doc, "Conclusion")
        addPhysicianNote(pdfDoc, doc)

        doc.close()
        pdfDoc.close()
        pdfWriter.close()
        fileOutputStream.close()

        "SUCCESS"
      }
      case (_, _, _) =>
        "No SOAP or PICO or Patient Found"
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

  private def feedPatientDetails(document : Document, patient : Patient) = {
    addHeading(document, "Background")
    addTitleWithParagraph(document, "Name", patient.name)
    addTitleWithParagraph(document, "Age", patient.age.toString)
    addTitleWithParagraph(document, "Address", patient.address)
  }

  private def feedSOAP(document: Document, soap : PatientSoap): Unit = {
    addHeading(document, "Case Presentation")
    addTitleWithParagraph(document, "Subject", soap.subjectiveNodeData.toString)
    addTitleWithParagraph(document, "Object", soap.objective.toString)
    addTitleWithParagraph(document, "Assessment", soap.assessment.toString)
    addTitleWithParagraph(document, "Plan", soap.plan.toString)
  }

  private def feedPICO(document: Document, pico : Pico): Unit = {
    addHeading(document, "Methods")
    addTitleWithParagraph(document, "Patient/Problem", pico.problem)
    addTitleWithParagraph(document, "Intervention", pico.intervention)
    addTitleWithParagraph(document, "Comparison", pico.comparison.getOrElse(""))
    addTitleWithParagraph(document, "Outcome", pico.outcome)
    addTitleWithParagraph(document, "Time", pico.timePeriod.getOrElse(""))

    addParagraph(document, "The Research data is gathered from the PubMed Database")
  }

  private def addTitleWithParagraph(document: Document, titleText : String, para: String): Unit = {
    val title = new Paragraph(titleText)
    title.setFont(PdfFontFactory.createFont(HELVETICA_BOLD))
    document.add(title)

    addParagraph(document, para)
  }

  private def addParagraph(document : Document, para:String): Unit = {
    val paragraph = new Paragraph(para)
    document.add(paragraph)
  }

  private def addHeading(document: Document, headingText: String ) {
    val heading = new Paragraph(headingText)
    heading.setFont(PdfFontFactory.createFont(HELVETICA_BOLD))
    heading.setFontSize(20f)
    document.add(heading)
  }

  private def feedArticleSummary(document : Document, articles: Seq[Article]): Unit = {
    addHeading(document, "Results")
    articles.filter(_.summary.nonEmpty).foreach(article => addParagraph(document, article.summary.get))
  }

  private def addPhysicianNote(pdf : PdfDocument, doc : Document): Unit = {
    val freeBBox = doc.getRenderer.getCurrentArea.getBBox
    val top = freeBBox.getTop
    val fieldHeight = 100f;
    val rect = new Rectangle(freeBBox.getLeft, top - fieldHeight, 550f, fieldHeight)
    val acroForm = PdfAcroForm.getAcroForm(pdf, true)
    val field = PdfFormField.createText(pdf, rect, "Conclusion", "")
    field.setMultiline(true)
    field.getWidgets.get(0).setBorderStyle(STYLE_BEVELED)
    field.setBorderWidth(2).setBorderColor(BLUE)
    acroForm.addField(field)
  }
}
