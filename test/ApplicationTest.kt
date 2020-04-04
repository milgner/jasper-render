package net.illunis

import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.utils.io.streams.asInput
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
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
                                Pair(HttpHeaders.ContentType,
                                    ContentType.fromFileExtension("json").map { it.toString() })
                            )
                        )
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
                assertEquals("application/pdf", response.headers["Content-Type"])
            }
        }
    }
}
