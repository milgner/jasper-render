package net.illunis

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveMultipart
import io.ktor.response.header
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperReport
import org.apache.pdfbox.pdmodel.PDDocument
import org.json.simple.JSONObject
import org.json.simple.parser.ParseException
import java.lang.Exception

class ReportRenderRequest(private val applicationCall: ApplicationCall) {
    companion object {
        val reportCache = ResourceCache("reports", "jrxml", JasperCompileManager::compileReport)
        val letterheadCache = ResourceCache("letterheads", "pdf", PDDocument::load)
    }

    private val reportName = applicationCall.parameters["report"]!!

    private var report: JasperReport? = null
    private var letterhead: PDDocument? = null

    suspend fun process() {
        try {
            loadResources()
            validateRequest()
            renderReport()
        } catch (err: ResourceCache.ResourceNotFoundException) {
            respondWithProblem("resource-not-found",
                "Resource not found",
                "The resource ${err.resourceName} was not found on the server",
                HttpStatusCode.NotFound
            )
        } catch(err: ParseException) {
            respondWithProblem("unparseable-request",
            "Cannot parse request",
            "Cannot parse JSON from request",
                HttpStatusCode.BadRequest
            )
        } catch(err: InvalidReportJsonException) {
            respondWithProblem(
                "invalid-json",
                "Erroneous JSON in request",
                err.errors.map { entry ->
                    entry.value.joinToString("\n") { "${entry.key} property: $it" }
                }.joinToString("\n"),
                HttpStatusCode.UnprocessableEntity
            )
        }
    }

    class InvalidReportJsonException(var errors: HashMap<String, List<String>>) : Exception("Invalid report JSON")

    private suspend fun renderReport() {
        var rendered = ReportRenderer(report!!).render(reportData!!)
        letterhead?.let { rendered = PdfLetterHeadMerge(it).use { merge -> merge.mergeOntoLetterhead(rendered) } }
        applicationCall.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "$reportName.pdf")
                .toString()
        )
        applicationCall.respondBytes(rendered,
            ContentType.Application.Pdf,
            HttpStatusCode.Created)
    }

    private var reportData: ReportData? = null
    private suspend fun validateRequest() {
        val json = UploadProcessor(reportName, applicationCall.receiveMultipart()).parseJsonFromRequest("report")
        if (json == null) {
            respondWithProblem(
                "report-key-not-found",
                "Report name key missing from JSON",
                "JSON is missing the key $reportName",
                HttpStatusCode.UnprocessableEntity
            )
            return
        }
        val errors = HashMap<String, List<String>>()
        missingParametersInRequest(json, report!!).let {
            if (it.isNotEmpty()) { errors["missing"] = it }
        }
        superfluousParametersInRequest(json, report!!).let {
            if (it.isNotEmpty()) { errors["unknown"] = it }
        }
        if (errors.size > 0) {
            throw InvalidReportJsonException(errors)
        }
        reportData = ReportData(json)
    }

    private fun superfluousParametersInRequest(json: JSONObject, report: JasperReport): List<String> {
        @Suppress("UNCHECKED_CAST")
        return json.keys.filter { key ->
            (key is String) && ((key != "Items") && report.parameters.none { param -> param.name == key })
        } as List<String>
    }

    private fun missingParametersInRequest(json: JSONObject, report: JasperReport): List<String> {
        return report.parameters.filter {
            !it.isSystemDefined &&
                    (it.defaultValueExpression == null || it.defaultValueExpression.text.isEmpty()) &&
                    !json.containsKey(it.name)
        }.map { it.name }
    }

    private fun loadResources() {
        if (report == null) {
            report = reportCache.load(reportName)
        }
        if (applicationCall.parameters.contains("letterhead") && letterhead == null) {
            letterhead = letterheadCache.load(applicationCall.parameters["letterhead"]!!)
        }
    }

    private suspend fun respondWithProblem(shortType: String,
                                           title: String,
                                           detail: String,
                                           statusCode: HttpStatusCode) {
        applicationCall.respondText(
            JSONObject.toJSONString(
                mapOf(
                    "type" to "http://jasper-render.illunis.net/problems/$shortType",
                    "title" to title,
                    "detail" to detail,
                    "status" to statusCode.value
                )
            ),
            ContentType.Application.ProblemJson, statusCode
        )
    }
}