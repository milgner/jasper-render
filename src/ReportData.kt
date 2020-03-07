package net.illunis

import org.apache.pdfbox.pdmodel.PDDocument
import org.json.simple.JSONObject

class ReportData(val jsonData: JSONObject, val letterhead: PDDocument? = null)