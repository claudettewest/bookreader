package com.claudettewest.offlinebookshelf.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.claudettewest.offlinebookshelf.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.security.MessageDigest
import java.util.UUID

data class ImportResult(val sourceName: String, val bookId: String? = null, val error: String? = null)

class BookImporter(private val context: Context, private val dao: LibraryDao) {
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "Untitled"
        val format = formatFor(name) ?: return@withContext ImportResult(name, error = "This file type is not supported.")
        runCatching {
            resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val id = UUID.randomUUID().toString()
            val root = File(context.filesDir, "books/$id").apply { mkdirs() }
            val original = File(root, "original/${safeName(name)}").apply { parentFile?.mkdirs() }
            resolver.openInputStream(uri)!!.use { input -> original.outputStream().use(input::copyTo) }
            val hash = original.inputStream().use { stream ->
                val digest = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(64 * 1024)
                while (true) { val read = stream.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            require(dao.byHash(hash) == null) { "This book is already in your library." }
            val parsed = when (format) {
                BookFormat.EPUB -> EpubParser().parse(original, root, id)
                BookFormat.TXT, BookFormat.MARKDOWN, BookFormat.HTML, BookFormat.RTF, BookFormat.FB2 -> TextParser().parse(original, root, id, format)
                BookFormat.PDF -> ParsedBook(name.substringBeforeLast('.'), "Unknown author", emptyList(), null, false)
            }
            val book = BookEntity(id, parsed.title.ifBlank { name.substringBeforeLast('.') }, parsed.author.ifBlank { "Unknown author" }, format = format, sourceUri = uri.toString(), sourceName = name, privatePath = original.absolutePath, coverPath = parsed.coverPath, contentHash = hash, fileSize = original.length(), chapterCount = parsed.chapters.size, searchable = format != BookFormat.PDF || parsed.searchable, ttsAvailable = format != BookFormat.PDF || parsed.searchable)
            dao.insertBook(book); dao.insertChapters(parsed.chapters)
            ImportResult(name, id)
        }.getOrElse { rootCause -> ImportResult(name, error = rootCause.message ?: "The book could not be imported.") }
    }

    private fun formatFor(name: String) = when (name.substringAfterLast('.', "").lowercase()) {
        "epub" -> BookFormat.EPUB; "pdf" -> BookFormat.PDF; "txt" -> BookFormat.TXT
        "html", "htm" -> BookFormat.HTML; "md", "markdown" -> BookFormat.MARKDOWN
        "rtf" -> BookFormat.RTF; "fb2" -> BookFormat.FB2; else -> null
    }
    private fun safeName(name: String) = name.replace(Regex("[^A-Za-z0-9._ -]"), "_")
}

data class ParsedBook(val title: String, val author: String, val chapters: List<ChapterEntity>, val coverPath: String?, val searchable: Boolean = true)

class TextParser {
    fun parse(file: File, root: File, id: String, format: BookFormat): ParsedBook {
        val raw = file.readText(Charsets.UTF_8)
        val text = when (format) {
            BookFormat.HTML, BookFormat.FB2 -> Jsoup.parse(raw).text()
            BookFormat.MARKDOWN -> raw.replace(Regex("[#*_>`]"), "")
            BookFormat.RTF -> raw.replace(Regex("\\\\[a-z]+\\d* ?|[{}]"), "")
            else -> raw
        }
        val chunks = text.chunked(80_000)
        val chapterDir = File(root, "chapters").apply { mkdirs() }
        val chapters = chunks.mapIndexed { index, chunk ->
            val out = File(chapterDir, "$index.txt").apply { writeText(chunk) }
            ChapterEntity(id, index, "section-$index", if (chunks.size == 1) "Book" else "Section ${index + 1}", out.absolutePath, chunk)
        }
        return ParsedBook(file.nameWithoutExtension, "Unknown author", chapters, null)
    }
}
