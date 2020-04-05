package net.illunis

import net.sf.jasperreports.engine.JRDataSource
import org.apache.pdfbox.pdmodel.PDDocument
import org.json.simple.JSONArray
import org.json.simple.JSONObject

class ReportData(val jsonData: JSONObject, val letterhead: PDDocument? = null) {
    fun getDataSource(): JRDataSource {
        return JsonItemDataSource(jsonData.get("Items") as JSONArray)
    }
}