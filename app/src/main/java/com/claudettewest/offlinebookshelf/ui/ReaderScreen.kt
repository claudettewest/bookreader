package com.claudettewest.offlinebookshelf.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import com.claudettewest.offlinebookshelf.BookshelfApplication
import com.claudettewest.offlinebookshelf.data.*
import com.claudettewest.offlinebookshelf.tts.TtsPlaybackService
import com.claudettewest.offlinebookshelf.importer.PdfTextExtractor
import com.claudettewest.offlinebookshelf.importer.PdfOcrExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.UUID
import java.io.File
import org.jsoup.Jsoup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(bookId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val readerPreferences = remember { context.getSharedPreferences("reader_preferences", android.content.Context.MODE_PRIVATE) }
    val dao = (context.applicationContext as BookshelfApplication).database.libraryDao()
    val scope = rememberCoroutineScope()
    val book by dao.observeBook(bookId).collectAsState(initial = null)
    val chapters by dao.observeChapters(bookId).collectAsState(initial = emptyList())
    val saved by dao.observePosition(bookId).collectAsState(initial = null)
    var chapter by remember(saved) { mutableIntStateOf(saved?.chapter ?: 0) }
    var pdfPage by remember(saved) { mutableIntStateOf(saved?.page ?: 0) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var controls by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(readerPreferences.getFloat("font_size", 20f)) }
    var lineHeight by remember { mutableFloatStateOf(readerPreferences.getFloat("line_spacing", 1.55f)) }
    var search by remember { mutableStateOf(false) }
    var toc by remember { mutableStateOf(false) }
    var readerTheme by remember { mutableStateOf(readerPreferences.getString("theme", "Light") ?: "Light") }
    val readerColors = when (readerTheme) {
        "Sepia" -> Color(0xFFF1E7D0) to Color(0xFF3E2F22)
        "Dark" -> Color(0xFF494C60) to Color(0xFFF9E4DF)
        "AMOLED" -> Color.Black to Color(0xFFF5F2EE)
        else -> Color(0xFFF9F6F2) to Color(0xFF1B1745)
    }
    val current = chapters.getOrNull(chapter)
    val chapterImages by produceState<List<File>>(emptyList(), current?.contentPath) {
        value = withContext(Dispatchers.IO) {
            current?.contentPath?.let { path ->
                runCatching {
                    val document = Jsoup.parse(File(path).readText())
                    document.select("img[src], image[href], image[xlink:href]").mapNotNull { element ->
                        val source = element.attr("src").ifBlank { element.attr("href") }.ifBlank { element.attr("xlink:href") }
                        File(source).takeIf(File::exists)
                    }.distinct()
                }.getOrDefault(emptyList())
            } ?: emptyList()
        }
    }
    val chapterParagraphs by produceState<List<String>>(emptyList(), current?.contentPath, current?.plainText) {
        value = withContext(Dispatchers.IO) {
            val blocks = current?.contentPath?.takeIf { it.endsWith(".html", true) }?.let { path ->
                runCatching { Jsoup.parse(File(path).readText()).select("h1,h2,h3,h4,p,li,blockquote").map { it.text().trim() }.filter(String::isNotBlank) }.getOrNull()
            }.orEmpty()
            val paragraphs = if (blocks.isNotEmpty()) blocks else current?.plainText?.split(Regex("\\n\\s*\\n")).orEmpty()
            paragraphs.dropWhile { it.equals(current?.title, ignoreCase = true) }
        }
    }

    fun save() {
        scope.launch {
            val progress = if (chapters.isEmpty()) 0f else chapter.toFloat() / chapters.size
            val isPdf = book?.format == BookFormat.PDF
            val savedProgress = if (isPdf && pdfPageCount > 0) pdfPage.toFloat() / pdfPageCount else progress
            dao.savePosition(ReadingPositionEntity(bookId, chapter = chapter, page = pdfPage, progress = savedProgress))
            book?.let { dao.updateBook(it.copy(progress = savedProgress, lastOpenedAt = System.currentTimeMillis())) }
        }
    }
    DisposableEffect(Unit) { onDispose { save() } }
    LaunchedEffect(book?.id) { book?.let { dao.updateBook(it.copy(lastOpenedAt = System.currentTimeMillis())) } }

    Scaffold(
        containerColor = readerColors.first,
        topBar = {
            if (controls) TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = readerColors.first, titleContentColor = readerColors.second, navigationIconContentColor = readerColors.second, actionIconContentColor = readerColors.second),
                title = { Text(current?.title ?: book?.title ?: "Reader", maxLines = 1) },
                navigationIcon = { IconButton(onClick = { save(); onBack() }) { Icon(Icons.Default.ArrowBack, "Back to library") } },
                actions = {
                    if (book?.format != BookFormat.PDF) IconButton(onClick = { toc = true }) { Icon(Icons.Default.List, "Table of contents") }
                    IconButton(onClick = { search = true }) { Icon(Icons.Default.Search, "Search within book") }
                    IconButton(onClick = {
                        current?.let { section -> scope.launch { dao.saveBookmark(BookmarkEntity(UUID.randomUUID().toString(), bookId, chapter = chapter, preview = section.plainText.take(120))) } }
                    }) { Icon(Icons.Default.BookmarkAdd, "Add bookmark") }
                    IconButton(onClick = { settings = true }) { Icon(Icons.Default.TextFields, "Reader settings") }
                }
            )
        },
        bottomBar = {
            if (controls) BottomAppBar(containerColor = readerColors.first, contentColor = readerColors.second) {
                IconButton(onClick = {
                    if (book?.format == BookFormat.PDF) { if (pdfPage > 0) pdfPage-- }
                    else if (chapter > 0) chapter--
                    save()
                }) { Icon(Icons.Default.SkipPrevious, if (book?.format == BookFormat.PDF) "Previous page" else "Previous chapter") }
                IconButton(onClick = {
                    scope.launch {
                        val text = if (book?.format == BookFormat.PDF) withContext(Dispatchers.IO) {
                            val embedded = PdfTextExtractor.extractAll(book!!.privatePath)
                            val devanagariCount = embedded.count { it in '\u0900'..'\u097F' }
                            if (devanagariCount >= 8) embedded else PdfOcrExtractor.extractHindiPage(book!!.privatePath, pdfPage)
                        }
                        else withContext(Dispatchers.IO) {
                            val remaining = dao.chapters(bookId).drop(chapter)
                            val sections = if (readerPreferences.getBoolean("continue_chapters", true)) remaining else remaining.take(1)
                            sections.joinToString("\n\n") { it.plainText }
                        }
                        context.startForegroundService(Intent(context, TtsPlaybackService::class.java).putExtra(TtsPlaybackService.EXTRA_TEXT, text).putExtra(TtsPlaybackService.EXTRA_BOOK_ID, bookId))
                    }
                }) { Icon(Icons.Default.PlayArrow, "Read aloud") }
                IconButton(onClick = {
                    context.startService(Intent(context, TtsPlaybackService::class.java).setAction(TtsPlaybackService.ACTION_STOP))
                }) { Icon(Icons.Default.StopCircle, "Stop read aloud") }
                Text(if (book?.format == BookFormat.PDF) "${pdfPage + 1} / $pdfPageCount" else "${chapter + 1} / ${chapters.size}", Modifier.weight(1f))
                IconButton(onClick = {
                    if (book?.format == BookFormat.PDF) { if (pdfPage < pdfPageCount - 1) pdfPage++ }
                    else if (chapter < chapters.lastIndex) chapter++
                    save()
                }) { Icon(Icons.Default.SkipNext, if (book?.format == BookFormat.PDF) "Next page" else "Next chapter") }
            }
        }
    ) { padding ->
        if (book?.format == BookFormat.PDF && book?.privatePath != null) {
            NativePdfReader(
                book!!.privatePath,
                pdfPage,
                onPageChanged = { page -> pdfPage = page; save() },
                onPageCount = { count -> pdfPageCount = count },
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize().background(readerColors.first).pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> fontSize = (fontSize * zoom).coerceIn(14f, 44f) } }.clickable { controls = !controls },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
            ) {
                items(chapterImages, key = { it.absolutePath }) { image ->
                    AsyncImage(image, "Illustration in ${current?.title.orEmpty()}", Modifier.fillMaxWidth().padding(bottom = 20.dp), contentScale = ContentScale.FillWidth)
                }
                item { Text(current?.title.orEmpty(), style = MaterialTheme.typography.headlineMedium, color = readerColors.second) }
                itemsIndexed(chapterParagraphs) { _, paragraph ->
                    Text(paragraph.trim(), color = readerColors.second, fontSize = fontSize.sp, lineHeight = (fontSize * lineHeight).sp, fontFamily = FontFamily.Serif, modifier = Modifier.padding(bottom = 18.dp))
                }
            }
        }
    }
    if (settings) ModalBottomSheet(onDismissRequest = { settings = false }) {
        Column(Modifier.padding(24.dp)) {
            Text("Reader settings", style = MaterialTheme.typography.titleLarge)
            Text("Text size ${fontSize.toInt()}"); Slider(fontSize, { fontSize = it }, valueRange = 14f..44f)
            Text("Line spacing"); Slider(lineHeight, { lineHeight = it }, valueRange = 1.1f..2.1f)
            Row { listOf("Light", "Sepia", "Dark", "AMOLED").forEach { theme -> FilterChip(readerTheme == theme, { readerTheme = theme; readerPreferences.edit().putString("theme", theme).apply() }, { Text(theme) }, Modifier.padding(end = 6.dp)) } }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (search) AlertDialog(onDismissRequest = { search = false }, confirmButton = { TextButton({ search = false }) { Text("Close") } }, title = { Text("Search within book") }, text = { Text("Search is indexed locally across ${chapters.size} sections.") })
    if (toc) ModalBottomSheet(onDismissRequest = { toc = false }) {
        Text("Table of contents", Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 600.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(chapters) { index, item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    leadingContent = { Text("${index + 1}") },
                    modifier = Modifier.clickable { chapter = index; save(); toc = false }
                )
            }
        }
    }
}
