package net.illunis

import org.apache.pdfbox.preflight.Format
import org.apache.pdfbox.preflight.PreflightDocument
import org.apache.pdfbox.preflight.ValidationResult
import org.apache.pdfbox.preflight.exception.SyntaxValidationException
import org.apache.pdfbox.preflight.parser.PreflightParser
import org.apache.pdfbox.preflight.utils.ByteArrayDataSource
import strikt.api.Assertion
import java.io.ByteArrayInputStream

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