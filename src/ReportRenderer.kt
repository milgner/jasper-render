package net.illunis

import net.sf.jasperreports.engine.JRParameter
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperReport

class ReportRenderer {
    companion object {
        fun render(report: JasperReport, data: ReportData): ByteArray {
            val params = report.parameters.associateBy({ it.name },
                { JsonUtils.extractValue(data.jsonData, it.name, it.valueClass) })
                .toMutableMap()
            params[JRParameter.REPORT_DATA_SOURCE] = data.getDataSource()
            val filled = JasperFillManager.fillReport(report, params)
            return ReportPdfExporter.exportToPdf(filled)
        }
    }
}