package net.illunis

import org.apache.pdfbox.multipdf.Overlay
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException


class PdfLetterHeadMerge(letterhead: String) : AutoCloseable {
    companion object {
        // TODO: this is the same pattern as in ReportRegistry. Unify it.
        private var cache = HashMap<String, PDDocument>()
        fun getLetterhead(name: String): PDDocument {
            if (!cache.containsKey(name)) {
                this::class.java.getResourceAsStream("/letterheads/$name.pdf").use { input ->
                    if (input == null) {
                        // TODO: maybe introduce custom exception classes to return proper HTTP status codes?
                        throw IOException("No letterhead named $name.pdf exists")
                    }
                    cache[name] = PDDocument.load(input)
                }
            }
            return cache[name]!!
        }
    }

    private var overlay: Overlay = Overlay();

    init {
        overlay.setAllPagesOverlayPDF(getLetterhead(letterhead))
    }

    override fun close() {
        overlay.close()
    }

    @Throws(IOException::class)
    fun mergeOntoLetterhead(originalPdfData: ByteArray): ByteArray {
        loadPdf(originalPdfData).use { pdf ->
            if (pdf.numberOfPages == 0) {
                return originalPdfData
            }
            overlay.setInputPDF(pdf)

            ByteArrayOutputStream().use { output ->
                overlay.overlay(HashMap()).use { overlayed ->
                    overlayed.save(output)
                }
                output.flush()
                return output.toByteArray()
            }
        }
    }

    private fun loadPdf(data: ByteArray): PDDocument {
        ByteArrayInputStream(data).use {
            return PDDocument.load(it)
        }
    }
}