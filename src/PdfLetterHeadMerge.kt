package net.illunis

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.apache.pdfbox.multipdf.Overlay
import org.apache.pdfbox.pdmodel.PDDocument

class PdfLetterHeadMerge(letterhead: PDDocument) : AutoCloseable {
    private var overlay: Overlay = Overlay()

    init {
        overlay.setAllPagesOverlayPDF(letterhead)
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
