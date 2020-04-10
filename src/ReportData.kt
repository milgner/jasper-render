package net.illunis

import net.sf.jasperreports.engine.JRDataSource
import org.json.simple.JSONArray
import org.json.simple.JSONObject

class ReportData(val jsonData: JSONObject) {
    fun getDataSource(): JRDataSource {
        return JsonItemsDataSource(jsonData.get("Items") as JSONArray)
    }
}
