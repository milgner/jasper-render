package net.illunis

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.preflight.Format
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
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
            }.apply {
                expect {
                    that(response.status()).isEqualTo(HttpStatusCode.Created)
                    that(response.headers["Content-Type"]).isEqualTo("application/pdf")
                    that(response.byteContent).isValidPdf(Format.PDF_A1A)
                }
                val pdfDocument = PDDocument.load(response.byteContent)
                expectThat(pdfDocument)
                    .containsText("Foobar Inc")
                    .containsText("Item 1")
                    .containsText("1.23")
            }
        }
    }
}
