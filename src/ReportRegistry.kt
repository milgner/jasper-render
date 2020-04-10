package net.illunis

import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperReport

class ReportRegistry {
    companion object {
        private val compileCache: MutableMap<String, JasperReport> = HashMap()

        fun load(name: String): net.sf.jasperreports.engine.JasperReport {
            val reportName = if (name.endsWith(".jrxml")) name else "$name.jrxml"

            if (!compileCache.containsKey(reportName)) {
                this::class.java.getResourceAsStream("/reports/$reportName").use { input ->
                    val report = JasperCompileManager.compileReport(input)
                    compileCache[reportName] = report
                }
            }
            return compileCache[reportName]!!
        }
    }
}
