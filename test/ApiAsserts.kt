package net.illunis

import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse
import org.apache.pdfbox.preflight.Format
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun Assertion.Builder<TestApplicationResponse>.returnsPdf(format: Format = Format.PDF_A1A) =
    assert("is a valid ${format.fname} response") {
        expectThat(it.status()).isEqualTo(HttpStatusCode.Created)
        expectThat(it.headers["Content-Type"]).isEqualTo("application/pdf")
        expectThat(it.byteContent).isValidPdf(format)
    }