package net.illunis

import net.sf.jasperreports.engine.JRParameter
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperPrint
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.export.JRPdfExporter
import net.sf.jasperreports.export.SimpleExporterInput
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput
import net.sf.jasperreports.export.SimplePdfExporterConfiguration
import net.sf.jasperreports.export.type.PdfVersionEnum
import net.sf.jasperreports.export.type.PdfaConformanceEnum
import org.json.simple.JSONObject
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

class ReportRenderer {
    companion object {
        fun render(report: JasperReport, data: ReportData): ByteArray {
            val params = report.parameters.associateBy({ it.name }, { extractParamFromData(it, data) })
            // TODO: implement JRDataSource which supplies data via Items array in JSON
            val filled = JasperFillManager.fillReport(report, params)
            return ReportPdfExporter.exportToPdf(filled)
        }

        private val STRING_CONVERTERS = mapOf<Class<*>, (Any) -> Any>(
            BigDecimal::class.java to { value -> BigDecimal(value as String) },
            Double::class.java to { value -> java.lang.Double.parseDouble(value as String) },
            Float::class.java to { value -> java.lang.Float.parseFloat(value as String) },
            Boolean::class.java to { value -> (value as String).toLowerCase().compareTo("true") == 0 },
            java.util.Date::class.java to { value -> java.time.format.DateTimeFormatter.ISO_DATE_TIME.parse(value as String) }
        )
        private val LONG_CONVERTERS = mapOf<Class<*>, (Any) -> Any>(
            Long::class.java to { it -> it },
            BigDecimal::class.java to { it -> BigDecimal(it as Long) },
            Double::class.java to { value -> (value as Long).toDouble() },
            Float::class.java to { value -> (value as Long).toFloat() }
        )

        private val BOOL_CONVERTERS = mapOf<Class<*>, (Any) -> Any>(
            Boolean::class.java to { value -> value },
            String::class.java to { value -> (value as String).trim().isNotEmpty() }
        )

        private val CONVERTERS = mapOf<Class<*>, Map<Class<*>, (Any) -> Any>>(
            JSONObject::class.java to emptyMap(),
            String::class.java to STRING_CONVERTERS,
            Long::class.java to LONG_CONVERTERS,
            Boolean::class.java to BOOL_CONVERTERS
        )

        private fun extractParamFromData(param: JRParameter, data: ReportData): Any? {
            if (!data.jsonData.containsKey(param.name)) {
                return null
            }
            val value = data.jsonData[param.name] ?: return null
            val converters = CONVERTERS.getOrDefault(value.javaClass, STRING_CONVERTERS)
            return (converters[param.valueClass] ?: error("Unsupported type conversion")).invoke(value)
        }

    }
}