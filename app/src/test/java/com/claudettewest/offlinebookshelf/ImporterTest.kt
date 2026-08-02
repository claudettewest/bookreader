package com.claudettewest.offlinebookshelf

import com.claudettewest.offlinebookshelf.data.BookFormat
import com.claudettewest.offlinebookshelf.importer.TextParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ImporterTest {
    @Test fun `large text is split into manageable sections`() {
        val temp = createTempDir(); val input = File(temp, "large.txt").apply { writeText("word ".repeat(40_000)) }
        val parsed = TextParser().parse(input, temp, "book", BookFormat.TXT)
        assertTrue(parsed.chapters.size > 1)
        assertEquals(input.nameWithoutExtension, parsed.title)
        temp.deleteRecursively()
    }

    @Test fun `markdown syntax is removed from searchable text`() {
        val temp = createTempDir(); val input = File(temp, "book.md").apply { writeText("# Heading\n\n**Strong words**") }
        val parsed = TextParser().parse(input, temp, "book", BookFormat.MARKDOWN)
        assertFalse(parsed.chapters.single().plainText.contains("**"))
        temp.deleteRecursively()
    }
}
