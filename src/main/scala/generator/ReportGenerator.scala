package generator

import com.google.common.io.ByteStreams
import com.google.common.io.ByteStreams.toByteArray
import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.kernel.pdf.{PdfDocument, PdfReader, PdfWriter}
import models.Pdf

import java.io.FileInputStream
import scala.reflect.io.File


object ReportGenerator {
  private val TEMPLATE_FILE_PATH: String = "/Report_template.pdf"

  def getReport(): Pdf = {
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
  }
}
