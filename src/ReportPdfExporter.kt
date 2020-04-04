package net.illunis

import net.sf.jasperreports.engine.JasperPrint
import net.sf.jasperreports.engine.export.JRPdfExporter
import net.sf.jasperreports.export.SimpleExporterInput
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput
import net.sf.jasperreports.export.SimplePdfExporterConfiguration
import net.sf.jasperreports.export.type.PdfVersionEnum
import net.sf.jasperreports.export.type.PdfaConformanceEnum
import java.io.ByteArrayOutputStream

class ReportPdfExporter {
    companion object {
        fun exportToPdf(filled: JasperPrint): ByteArray {
            val exporter = JRPdfExporter()
            exporter.setExporterInput(SimpleExporterInput(filled))
            val exporterConfiguration = SimplePdfExporterConfiguration()
            exporterConfiguration.isCompressed = true
            exporterConfiguration.pdfaConformance = PdfaConformanceEnum.PDFA_1A
            // TODO: for now just take the first ICC file found, later we might use headers or JSON data to select a specific one
            exporterConfiguration.iccProfilePath = getResourceList("icc").find { it.endsWith(".icc") }
            exporterConfiguration.isCompressed = false
            exporterConfiguration.isEncrypted = false
            exporterConfiguration.isTagged = true
            exporterConfiguration.pdfVersion = PdfVersionEnum.VERSION_1_4
            exporter.setConfiguration(exporterConfiguration)
            val outputStream = ByteArrayOutputStream()
            exporter.exporterOutput = SimpleOutputStreamExporterOutput(outputStream)
            exporter.exportReport()
            outputStream.flush()
            return outputStream.toByteArray()
        }

        private fun getResourceList(path: String): List<String> {
            val stream = this::class.java.getResourceAsStream("/$path") ?: return emptyList()
            return stream.bufferedReader().use {
                it.readLines().map { file -> javaClass.classLoader.getResource("$path/$file")!!.file }
            }
        }
    }
}