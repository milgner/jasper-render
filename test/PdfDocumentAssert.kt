package net.illunis

import java.io.ByteArrayInputStream
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.preflight.Format
import org.apache.pdfbox.preflight.ValidationResult
import org.apache.pdfbox.preflight.exception.SyntaxValidationException
import org.apache.pdfbox.preflight.parser.PreflightParser
import org.apache.pdfbox.preflight.utils.ByteArrayDataSource
import org.apache.pdfbox.text.PDFTextStripper
import strikt.api.Assertion

fun Assertion.Builder<ByteArray?>.isValidPdf(format: Format) =
    assert("is a valid ${format.fname} document") {
        if (it == null) {
            fail(description = "Is null")
        }
        val byteArrayDataSource = ByteArrayDataSource(ByteArrayInputStream(it))
        val parser = PreflightParser(byteArrayDataSource)
        try {
            parser.parse(format)
            parser.preflightDocument.use { document ->
                document.validate()
                val result: ValidationResult = document.result
                if (!result.isValid) {
                    val errorDetails = result.errorsList.map { ed -> ed.details.toString() }
                    fail(description = errorDetails.joinToString("\n"))
                }
            }
        } catch (e: SyntaxValidationException) {
            fail(description = "Parsing PDF failed: ${e.message}")
        }
    }

fun Assertion.Builder<PDDocument>.containsText(text: String) =
    assert("contains \"$text\"") {
        val textStripper = PDFTextStripper()

        val stringContent = textStripper.getText(it)
        if (stringContent.contains(text)) {
            pass()
        } else {
            fail()
        }
    }
