package net.illunis

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.util.asStream
import kotlinx.coroutines.runBlocking
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class UploadProcessor(applicationCall: ApplicationCall) {

    private class InputStreamWithEncoding(val inputStream: InputStream, val encoding: String)

    private val call = applicationCall
    private val reportName = applicationCall.parameters["report"]!!

    fun parse(): ReportData {
        val json = reportJsonFromRequest() ?: throw IllegalArgumentException()

        return ReportData(json, null)
    }

    private fun reportJsonFromRequest(): JSONObject? {
        val streamWithEncoding = readMultipartByType(ContentType.Application.Json) ?: return null
        return readJson(streamWithEncoding)
    }

    private var multiPartData: List<PartData>? = null
    private fun getMultipartData(): List<PartData> {
        if (multiPartData == null) {
            runBlocking { multiPartData = call.receiveMultipart().readAllParts() }
        }
        return multiPartData ?: emptyList()
    }

    private fun readMultipartByType(
        contentType: ContentType,
        name: String? = null
    ): InputStreamWithEncoding? {
        val relevantPart = getMultipartData().find { part ->
            (part.contentType == contentType) && (name == null || part.name == name)
        } ?: return null

        val encoding = relevantPart.headers[HttpHeaders.ContentEncoding] ?: "UTF-8"

        val inputStream = when (relevantPart) {
            is PartData.FileItem -> {
                relevantPart.streamProvider()
            }
            is PartData.BinaryItem -> {
                relevantPart.provider().asStream()
            }
            else -> {
                throw IllegalArgumentException()
            }
        }
        return InputStreamWithEncoding(inputStream, encoding)
    }

    private fun readJson(data: InputStreamWithEncoding): JSONObject? {
        return try {
            val json = JSONParser().parse(InputStreamReader(data.inputStream, data.encoding))
            // it might be an array which isn't supported for the time being (will be added for batch support)
            // TODO: introduce proper errors so we can distinguish error codes and raise a proper 422 here
            if (json is JSONObject && json.containsKey(reportName) && json[reportName] is JSONObject) {
                json[reportName] as JSONObject
            } else {
                throw IllegalArgumentException()
            }
        } catch (e: IOException) {
            null
        } catch (e: ParseException) {
            null
        }
    }
}