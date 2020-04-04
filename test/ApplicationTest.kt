package net.illunis

import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.utils.io.streams.asInput
import kotlin.test.Test
import strikt.api.*
import strikt.assertions.*


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
                                    ContentType.fromFileExtension("json").map(ContentType::toString))
                            )
                        )
                    )
                )
            }.apply {
                expect {
                    that(response.status()).isEqualTo(HttpStatusCode.Created)
                    that(response.headers["Content-Type"]).isEqualTo("application/pdf")
                }
            }
        }
    }
}
