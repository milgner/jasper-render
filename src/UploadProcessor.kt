package net.illunis

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.util.asStream
import org.apache.pdfbox.pdmodel.PDDocument
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.InputStream
import java.io.InputStreamReader

class UploadProcessor {
    companion object {
        suspend fun parse(data: MultiPartData): ReportData {
            var json: JSONObject? = null
            var letterhead: PDDocument? = null
            data.forEachPart {
                val encoding = it.headers[HttpHeaders.ContentEncoding] ?: "UTF-8"
                val inputStream: InputStream = when (it) {
                    is PartData.FileItem -> {
                        it.streamProvider()
                    }
                    is PartData.BinaryItem -> {
                        it.provider().asStream()
                    }
                    else -> {
                        throw IllegalArgumentException()
                    }
                }
                when (it.contentType) {
                    ContentType.Application.Pdf -> {
                        letterhead = PDDocument.load(inputStream)
                    }
                    ContentType.Application.Json -> {
                        json = readJsonFromStream(inputStream, encoding)
                    }
                    else -> {
                        throw IllegalArgumentException()
                    }
                }
            }
            if (json == null) {
                throw IllegalArgumentException()
            }
            return ReportData(json!!, letterhead)
        }

        private fun readJsonFromStream(stream: InputStream, encoding: String = "UTF-8"): JSONObject {
            val jsonParser = JSONParser()
            val parsed = jsonParser.parse(InputStreamReader(stream, encoding))
            when (parsed) {
                is JSONObject -> {
                    return parsed
                }
                else -> {
                    throw IllegalArgumentException("Invalid JSON content")
                }
            }
        }
    }
}