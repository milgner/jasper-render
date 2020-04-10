package net.illunis

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.fromFileExtension
import io.ktor.http.headersOf
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.utils.io.streams.asInput
import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import strikt.api.expectThat
import strikt.assertions.isEqualTo

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
                sendTestJson("invoice")
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

    private val utf8ProblemJson = ContentType.Application.ProblemJson.withParameter("charset", "UTF-8")

    @Test
    fun testInvalidJson() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Post, "/render/invoice") {
                sendTestJson("invalid")
            }.apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.BadRequest)
                expectThat(response.contentType()).isEqualTo(utf8ProblemJson)
            }
        }
    }

    @Test
    fun testIncompleteJson() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Post, "/render/invoice") {
                sendTestJson("missing_param")
            }.apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.UnprocessableEntity)
                expectThat(response.contentType()).isEqualTo(utf8ProblemJson)
            }
        }
    }

    @Test
    fun testSuperfluousParameter() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Post, "/render/invoice") {
                sendTestJson("unknown_param")
            }.apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.UnprocessableEntity)
                expectThat(response.contentType()).isEqualTo(utf8ProblemJson)
            }
        }
    }

    @Test
    fun testLetterheadMerge() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Post, "/render/invoice?letterhead=example") {
                sendTestJson("invoice")
            }.apply {
                expectThat(response).returnsPdf()
                val pdfDocument = PDDocument.load(response.byteContent)
                expectThat(pdfDocument)
                    .containsText("Example Corp")
                    .containsText("Sample Street")
            }
        }
    }

    private fun reportUploadFromResource(filename: String): PartData {
        return PartData.FileItem(
            { this::class.java.getResourceAsStream("/$filename.json").asInput() },
            {},
            headersOf(
                HttpHeaders.ContentDisposition to
                        listOf(
                            ContentDisposition.File
                                .withParameter(ContentDisposition.Parameters.Name, "report")
                                .withParameter(ContentDisposition.Parameters.FileName, "report.json")
                                .toString()
                        ),
                HttpHeaders.ContentType to
                        ContentType.fromFileExtension("json").map(ContentType::toString)
            )
        )
    }

    val testBoundary = "***REPORT-TESTING***"
    private fun TestApplicationRequest.sendTestJson(filename: String) {
        addHeader(
            HttpHeaders.ContentType,
            ContentType.MultiPart.FormData.withParameter("boundary", testBoundary).toString()
        )
        setBody(
            testBoundary, listOf(reportUploadFromResource(filename))
        )
    }
}
