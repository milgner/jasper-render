package net.illunis

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import strikt.api.expectThat
import java.io.File
import kotlin.test.Test

class ApplicationTest {
    init {
        if (this.javaClass.classLoader.getResource("icc/srgb.icc") == null) {
            val iccPath = this.javaClass.classLoader.getResource("icc")!!.path + "/srgb.icc"
            val icc = HttpClient().use { client ->
                runBlocking {
                    client.get<ByteArray>("http://www.color.org/profiles/sRGB2014.icc")
                }
            }
            File(iccPath).writeBytes(icc)
        }
    }

    @Test
    fun testRender() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Post, "/render/invoice") {
                sendInvoiceTestJson()
            }.apply {
                expectThat(response).returnsPdf()
                val pdfDocument = PDDocument.load(response.byteContent)
                expectThat(pdfDocument)
                    .containsText("Foobar Inc")
                    .containsText("Item 1")
                    .containsText("1.23")
            }
        }
    }

    @Test
    fun testLetterheadMerge() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Post, "/render/invoice?letterhead=example") {
                sendInvoiceTestJson()
            }.apply {
                expectThat(response).returnsPdf()
                val pdfDocument = PDDocument.load(response.byteContent)
                expectThat(pdfDocument)
                    .containsText("Example Corp")
                    .containsText("Sample Street")
            }
        }
    }

    private fun TestApplicationRequest.sendInvoiceTestJson() {
        val boundary = "***reportfoo***"
        addHeader(
            HttpHeaders.ContentType,
            ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString()
        )
        setBody(
            boundary, listOf(
                PartData.FileItem(
                    { this::class.java.getResourceAsStream("/invoice.json").asInput() },
                    {},
                    headersOf(
                        Pair(
                            HttpHeaders.ContentDisposition,
                            listOf(
                                ContentDisposition.File
                                    .withParameter(ContentDisposition.Parameters.Name, "report")
                                    .withParameter(ContentDisposition.Parameters.FileName, "report.json")
                                    .toString()
                            )
                        ),
                        Pair(
                            HttpHeaders.ContentType,
                            ContentType.fromFileExtension("json").map(ContentType::toString)
                        )
                    )
                )
            )
        )
    }
}
