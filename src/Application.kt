package net.illunis

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.Compression
import io.ktor.features.deflate
import io.ktor.features.gzip
import io.ktor.features.minimumSize
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(Authentication) {
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }
        post("/render/{report}") {
            val reportName = call.parameters["report"]!!
            val report = try {
                ReportRegistry.load(reportName)
            } catch (err: Throwable) {
                call.respondText(
                    "{\"message\": \"no such report: $reportName\"}",
                    status = HttpStatusCode.NotFound,
                    contentType = ContentType.Application.Json
                )
                return@post
            }

            val input = UploadProcessor(call).parse()
            var rendered = ReportRenderer.render(report, input)
            // TODO: ideally validate letterhead name before anything else, too
            if (call.parameters.contains("letterhead")) {
                PdfLetterHeadMerge(call.parameters["letterhead"]!!).use { merge ->
                    rendered = merge.mergeOntoLetterhead(rendered)
                }
            }
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "$reportName.pdf")
                    .toString()
            )
            call.respondBytes(rendered, contentType = ContentType.Application.Pdf, status = HttpStatusCode.Created)
        }
    }
}

