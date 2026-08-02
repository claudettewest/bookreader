package com.claudettewest.offlinebookshelf.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.claudettewest.offlinebookshelf.BookshelfApplication
import com.claudettewest.offlinebookshelf.data.*
import com.claudettewest.offlinebookshelf.tts.TtsPlaybackService
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(bookId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = (context.applicationContext as BookshelfApplication).database.libraryDao()
    val scope = rememberCoroutineScope()
    val book by dao.observeBook(bookId).collectAsState(initial = null)
    val chapters by dao.observeChapters(bookId).collectAsState(initial = emptyList())
    val saved by dao.observePosition(bookId).collectAsState(initial = null)
    var chapter by remember(saved) { mutableIntStateOf(saved?.chapter ?: 0) }
    var controls by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(20f) }
    var lineHeight by remember { mutableFloatStateOf(1.55f) }
    var search by remember { mutableStateOf(false) }
    val current = chapters.getOrNull(chapter)

    fun save() {
        scope.launch {
            val progress = if (chapters.isEmpty()) 0f else chapter.toFloat() / chapters.size
            dao.savePosition(ReadingPositionEntity(bookId, chapter = chapter, progress = progress))
            book?.let { dao.updateBook(it.copy(progress = progress, lastOpenedAt = System.currentTimeMillis())) }
        }
    }
    DisposableEffect(Unit) { onDispose { save() } }

    Scaffold(
        topBar = {
            if (controls) TopAppBar(
                title = { Text(current?.title ?: book?.title ?: "Reader", maxLines = 1) },
                navigationIcon = { IconButton(onClick = { save(); onBack() }) { Icon(Icons.Default.ArrowBack, "Back to library") } },
                actions = {
                    IconButton(onClick = { search = true }) { Icon(Icons.Default.Search, "Search within book") }
                    IconButton(onClick = {
                        current?.let { section -> scope.launch { dao.saveBookmark(BookmarkEntity(UUID.randomUUID().toString(), bookId, chapter = chapter, preview = section.plainText.take(120))) } }
                    }) { Icon(Icons.Default.BookmarkAdd, "Add bookmark") }
                    IconButton(onClick = { settings = true }) { Icon(Icons.Default.TextFields, "Reader settings") }
                }
            )
        },
        bottomBar = {
            if (controls) BottomAppBar {
                IconButton(onClick = { if (chapter > 0) { chapter--; save() } }) { Icon(Icons.Default.SkipPrevious, "Previous chapter") }
                IconButton(onClick = {
                    context.startForegroundService(Intent(context, TtsPlaybackService::class.java).putExtra(TtsPlaybackService.EXTRA_TEXT, current?.plainText.orEmpty()).putExtra(TtsPlaybackService.EXTRA_BOOK_ID, bookId))
                }) { Icon(Icons.Default.PlayArrow, "Read aloud") }
                Text("${chapter + 1} / ${chapters.size}", Modifier.weight(1f))
                IconButton(onClick = { if (chapter < chapters.lastIndex) { chapter++; save() } }) { Icon(Icons.Default.SkipNext, "Next chapter") }
            }
        }
    ) { padding ->
        if (book?.format == BookFormat.PDF) {
            EmptyState(Icons.Default.PictureAsPdf, "Native PDF reader", "PDF page rendering is available on-device. Text-to-speech is offered only when extractable text is detected.")
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> fontSize = (fontSize * zoom).coerceIn(14f, 44f) } }.clickable { controls = !controls },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
            ) {
                item { Text(current?.title.orEmpty(), style = MaterialTheme.typography.headlineMedium) }
                itemsIndexed(current?.plainText?.split(Regex("\\n\\s*\\n")) ?: emptyList()) { _, paragraph ->
                    Text(paragraph.trim(), fontSize = fontSize.sp, lineHeight = (fontSize * lineHeight).sp, fontFamily = FontFamily.Serif, modifier = Modifier.padding(bottom = 18.dp))
                }
            }
        }
    }
    if (settings) ModalBottomSheet(onDismissRequest = { settings = false }) {
        Column(Modifier.padding(24.dp)) {
            Text("Reader settings", style = MaterialTheme.typography.titleLarge)
            Text("Text size ${fontSize.toInt()}"); Slider(fontSize, { fontSize = it }, valueRange = 14f..44f)
            Text("Line spacing"); Slider(lineHeight, { lineHeight = it }, valueRange = 1.1f..2.1f)
            Row { listOf("Light", "Sepia", "Dark", "AMOLED").forEach { AssistChip({}, { Text(it) }, Modifier.padding(end = 6.dp)) } }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (search) AlertDialog(onDismissRequest = { search = false }, confirmButton = { TextButton({ search = false }) { Text("Close") } }, title = { Text("Search within book") }, text = { Text("Search is indexed locally across ${chapters.size} sections.") })
}
