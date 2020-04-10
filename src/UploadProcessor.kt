package net.illunis

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.util.asStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.runBlocking
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

class UploadProcessor(private val reportName: String, multiPartData: MultiPartData) {

    private class InputStreamWithEncoding(val inputStream: InputStream, val encoding: String)

    private var allParts: List<PartData>? = null

    init {
        runBlocking {
            allParts = multiPartData.readAllParts()
        }
    }

    class RequestPartNotFound(contentType: ContentType, partName: String?) :
        IOException(
            if (partName == null) {
                "Request part of type $contentType missing from request"
            } else {
                "Request part $partName of type $contentType missing from request"
            }
        )

    @Throws(RequestPartNotFound::class)
    private fun readMultipartByType(
        contentType: ContentType,
        name: String? = null
    ): InputStreamWithEncoding? {
        val relevantPart = allParts!!.find { part ->
            (part.contentType == contentType) && (name == null || part.name == name)
        } ?: throw(RequestPartNotFound(contentType, name))

        val encoding = relevantPart.headers[HttpHeaders.ContentEncoding] ?: "UTF-8"

        val inputStream = when (relevantPart) {
            is PartData.FileItem -> {
                relevantPart.streamProvider()
            }
            is PartData.BinaryItem -> {
                relevantPart.provider().asStream()
            }
            is PartData.FormItem -> {
                ByteArrayInputStream(relevantPart.value.toByteArray(StandardCharsets.UTF_8))
            }
            else -> {
                throw RequestPartNotFound(contentType, name)
            }
        }
        return InputStreamWithEncoding(inputStream, encoding)
    }

    @Throws(ParseException::class)
    fun parseJsonFromRequest(name: String): JSONObject? {
        val data = readMultipartByType(ContentType.Application.Json, name) ?: return null
        InputStreamReader(data.inputStream, data.encoding).use { input ->
            val json = JSONParser().parse(input)
            if (json is JSONObject && json.containsKey(reportName) && json[reportName] is JSONObject) {
                return json[reportName] as JSONObject
            }
            return null
        }
    }
}
