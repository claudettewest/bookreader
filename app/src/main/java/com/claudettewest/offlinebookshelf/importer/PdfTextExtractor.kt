package com.claudettewest.offlinebookshelf.importer

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

object PdfTextExtractor {
    fun extractAll(path: String): String = PDDocument.load(File(path)).use { document ->
        PDFTextStripper().apply { sortByPosition = true }.getText(document).trim()
    }

    fun extractPage(path: String, page: Int): String = PDDocument.load(File(path)).use { document ->
        PDFTextStripper().apply {
            sortByPosition = true
            startPage = page + 1
            endPage = page + 1
        }.getText(document).trim()
    }
}
