package com.claudettewest.offlinebookshelf.ui

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil3.compose.AsyncImage
import com.claudettewest.offlinebookshelf.data.BookEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.util.Locale
import com.claudettewest.offlinebookshelf.tts.TtsVoicePreferences

@Composable fun BookshelfApp(vm: LibraryViewModel) {
    val nav = rememberNavController(); val state by vm.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> vm.import(uris) }
    Scaffold(bottomBar = {
        NavigationBar {
            listOf("library" to (Icons.Default.Home to "Library"), "reading" to (Icons.Default.MenuBook to "Reading"), "bookmarks" to (Icons.Default.Bookmark to "Bookmarks"), "settings" to (Icons.Default.Settings to "Settings")).forEach { (route, item) ->
                NavigationBarItem(selected = nav.currentBackStackEntryAsState().value?.destination?.route == route, onClick = { nav.navigate(route) { launchSingleTop = true; popUpTo("library") } }, icon = { Icon(item.first, null) }, label = { Text(item.second) })
            }
        }
    }) { padding ->
        NavHost(nav, "library", Modifier.padding(padding)) {
            composable("library") { LibraryScreen(state, vm, onImport = { launcher.launch(arrayOf("application/epub+zip", "application/pdf", "text/plain", "text/html", "text/markdown", "application/rtf", "application/xml")) }, onOpen = { nav.navigate("reader/${it.id}") }) }
            composable("reading") { CurrentlyReadingScreen(state, onOpen = { nav.navigate("reader/${it.id}") }) }
            composable("bookmarks") { EmptyState(Icons.Default.Bookmark, "Bookmarks", "Your saved places will appear here.") }
            composable("settings") { SettingsScreen() }
            composable("reader/{bookId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { ReaderScreen(it.arguments?.getString("bookId")!!, onBack = nav::popBackStack) }
        }
    }
    state.message?.let { message -> Snackbar(Modifier.padding(16.dp), action = { TextButton(vm::clearMessage) { Text("OK") } }) { Text(message) } }
}

@Composable private fun CurrentlyReadingScreen(state: LibraryState, onOpen: (BookEntity) -> Unit) {
    val current = state.books.filter { it.lastOpenedAt != null }.maxByOrNull { it.lastOpenedAt ?: 0L }
    if (current == null) EmptyState(Icons.Default.MenuBook, "Currently reading", "Open a book from your library to continue.")
    else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Currently reading", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { BookRow(current, onOpen) }
        item { Button(onClick = { onOpen(current) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Continue reading") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LibraryScreen(state: LibraryState, vm: LibraryViewModel, onImport: () -> Unit, onOpen: (BookEntity) -> Unit) {
    var deleteTarget by remember { mutableStateOf<BookEntity?>(null) }
    Scaffold(floatingActionButton = { ExtendedFloatingActionButton(onClick = onImport, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Import book") }) }, topBar = { TopAppBar(title = { Column { Text("Read Aloud Library", fontWeight = FontWeight.Bold); Text("Your private library", style = MaterialTheme.typography.labelMedium) } }, actions = { IconButton(vm::toggleView) { Icon(if (state.grid) Icons.Default.ViewList else Icons.Default.GridView, "Change library layout") } }) }) { insets ->
        Column(Modifier.padding(insets).fillMaxSize()) {
            OutlinedTextField(state.query, vm::query, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search title or author") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(SortMode.entries) { sort -> FilterChip(state.sort == sort, { vm.sort(sort) }, { Text(sort.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) }) } }
            if (state.importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.books.isEmpty()) EmptyState(Icons.Default.AutoStories, "Your shelf is waiting", "Import EPUB, PDF, TXT, HTML, Markdown, RTF, or FB2 files. Everything stays on this device.")
            else if (state.grid) LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(state.books, key = { it.id }) { BookCard(it, onOpen, onDelete = { deleteTarget = it }) } }
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(state.books, key = { it.id }) { BookRow(it, onOpen, onDelete = { deleteTarget = it }) } }
        }
    }
    deleteTarget?.let { book -> AlertDialog(
        onDismissRequest = { deleteTarget = null },
        icon = { Icon(Icons.Default.DeleteOutline, null) },
        title = { Text("Remove this book?") },
        text = { Text("${book.title} and its bookmarks, reading position, extracted images, and private app copy will be removed. The original file you imported will not be deleted.") },
        dismissButton = { TextButton({ deleteTarget = null }) { Text("Cancel") } },
        confirmButton = { Button({ deleteTarget = null; vm.delete(book) }) { Text("Remove book") } }
    ) }
}

@Composable private fun BookCover(book: BookEntity, modifier: Modifier = Modifier) { Box(modifier.clip(RoundedCornerShape(12.dp)).background(when (book.title.hashCode().mod(3)) { 0 -> Silver; 1 -> TaupeGray; else -> Charcoal }), contentAlignment = Alignment.Center) { if (book.coverPath != null) AsyncImage(File(book.coverPath), "Cover of ${book.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AutoStories, null, tint = Platinum); Spacer(Modifier.height(8.dp)); Text(book.title, color = Platinum, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun BookCard(book: BookEntity, onOpen: (BookEntity)->Unit, onDelete: ((BookEntity)->Unit)? = null) { Column { Box { BookCover(book, Modifier.fillMaxWidth().aspectRatio(.68f).clickable { onOpen(book) }); onDelete?.let { delete -> IconButton({ delete(book) }, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.DeleteOutline, "Remove ${book.title}", tint = Platinum) } } }; Spacer(Modifier.height(8.dp)); Text(book.title, Modifier.clickable { onOpen(book) }, fontWeight = FontWeight.SemiBold, maxLines = 2); Text(book.author, style = MaterialTheme.typography.bodySmall, maxLines = 1); LinearProgressIndicator({ book.progress }, Modifier.fillMaxWidth().padding(top=6.dp)); Text(book.format.name, style = MaterialTheme.typography.labelSmall) } }
@Composable private fun BookRow(book: BookEntity, onOpen: (BookEntity)->Unit, onDelete: ((BookEntity)->Unit)? = null) { Card(onClick = { onOpen(book) }) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { BookCover(book, Modifier.width(64.dp).height(92.dp)); Column(Modifier.padding(start=14.dp).weight(1f)) { Text(book.title, fontWeight=FontWeight.Bold); Text(book.author); Spacer(Modifier.weight(1f)); LinearProgressIndicator({book.progress}, Modifier.fillMaxWidth()); Text("${(book.progress*100).toInt()}% • ${book.format.name}", style=MaterialTheme.typography.labelSmall) }; onDelete?.let { delete -> IconButton({ delete(book) }) { Icon(Icons.Default.DeleteOutline, "Remove ${book.title}") } } } } }

@Composable fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(56.dp), tint=TaupeGray); Spacer(Modifier.height(16.dp)); Text(title, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold); Text(body, Modifier.padding(top=8.dp), style=MaterialTheme.typography.bodyMedium) } } }

@Composable private fun SettingsScreen() {
    val context = LocalContext.current
    var voicePicker by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val exportSettings = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let {
            runCatching {
                val reader = context.getSharedPreferences("reader_preferences", android.content.Context.MODE_PRIVATE)
                val properties = java.util.Properties().apply {
                    setProperty("theme", reader.getString("theme", "Light"))
                    setProperty("font_size", reader.getFloat("font_size", 20f).toString())
                    setProperty("line_spacing", reader.getFloat("line_spacing", 1.55f).toString())
                    setProperty("continue_chapters", reader.getBoolean("continue_chapters", true).toString())
                    setProperty("speech_rate", TtsVoicePreferences.speechRate(context).toString())
                    setProperty("selected_voice", TtsVoicePreferences.selectedVoice(context).orEmpty())
                }
                context.contentResolver.openOutputStream(it)?.use { output -> properties.store(output, "Read Aloud Library settings backup") }
            }.onSuccess { status = "Settings backup exported." }.onFailure { status = "The settings backup could not be exported." }
        }
    }
    val restoreSettings = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                val properties = java.util.Properties().apply { context.contentResolver.openInputStream(it)?.use(::load) }
                context.getSharedPreferences("reader_preferences", android.content.Context.MODE_PRIVATE).edit()
                    .putString("theme", properties.getProperty("theme", "Light"))
                    .putFloat("font_size", properties.getProperty("font_size", "20").toFloat())
                    .putFloat("line_spacing", properties.getProperty("line_spacing", "1.55").toFloat())
                    .putBoolean("continue_chapters", properties.getProperty("continue_chapters", "true").toBoolean()).apply()
                TtsVoicePreferences.setSpeechRate(context, properties.getProperty("speech_rate", "1").toFloat())
                TtsVoicePreferences.selectVoice(context, properties.getProperty("selected_voice").orEmpty().ifBlank { null })
            }.onSuccess { status = "Settings restored." }.onFailure { status = "That file is not a valid settings backup." }
        }
    }
    LazyColumn(contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold) }
        item { Card(onClick = { voicePicker = true }) { ListItem(headlineContent={Text("Text-to-speech voice")}, supportingContent = { Text("Choose any voice installed on this device") }, leadingContent={Icon(Icons.Default.RecordVoiceOver,null)}, trailingContent={Icon(Icons.Default.ChevronRight,null)}) } }
        items(listOf(Icons.Default.TextFields to "Default reader settings", Icons.Default.PlayCircle to "Playback behaviour", Icons.Default.Storage to "Storage management", Icons.Default.Backup to "Backup and restore", Icons.Default.PrivacyTip to "Privacy", Icons.Default.Info to "About")) { (icon, label) -> Card(onClick = { panel = label }) { ListItem(headlineContent={Text(label)}, leadingContent={Icon(icon,null)}, trailingContent={Icon(Icons.Default.ChevronRight,null)}) } }
        item { Text("Private by design", fontWeight=FontWeight.Bold); Text("Books, bookmarks, reading history, and listening positions remain on this device. Offline Bookshelf has no account, ads, analytics, tracking, cloud sync, or Internet permission.") }
    }
    if (voicePicker) VoicePicker(onDismiss = { voicePicker = false })
    panel?.let { SettingsPanel(it, onDismiss = { panel = null }, onExport = { exportSettings.launch("read-aloud-settings.properties") }, onRestore = { restoreSettings.launch(arrayOf("text/plain", "application/octet-stream")) }) }
    status?.let { AlertDialog(onDismissRequest = { status = null }, confirmButton = { TextButton({ status = null }) { Text("OK") } }, text = { Text(it) }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun VoicePicker(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var voices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var selected by remember { mutableStateOf(TtsVoicePreferences.selectedVoice(context)) }
    var speed by remember { mutableFloatStateOf(TtsVoicePreferences.speechRate(context)) }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) voices = engine?.voices.orEmpty().sortedWith(compareBy({ it.locale.displayLanguage }, { it.name }))
        }
        engine = tts
        onDispose { tts.shutdown() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Choose TTS voice", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge)
        Text("The app automatically chooses Hindi unless you select a specific voice.", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Text("Speech speed: ${String.format(Locale.getDefault(), "%.1fx", speed)}", Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        Slider(speed, { speed = it; TtsVoicePreferences.setSpeechRate(context, it) }, Modifier.padding(horizontal = 24.dp), valueRange = 0.5f..2f, steps = 5)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 600.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { ListItem(headlineContent = { Text("Automatic language voice") }, supportingContent = { Text("Recommended") }, leadingContent = { RadioButton(selected == null, { selected = null; TtsVoicePreferences.selectVoice(context, null) }) }, modifier = Modifier.clickable { selected = null; TtsVoicePreferences.selectVoice(context, null) }) }
            items(voices, key = { it.name }) { voice ->
                ListItem(headlineContent = { Text("${voice.locale.displayLanguage} (${voice.locale.displayCountry})") }, supportingContent = { Text(voice.name) }, leadingContent = { RadioButton(selected == voice.name, { selected = voice.name; TtsVoicePreferences.selectVoice(context, voice.name) }) }, modifier = Modifier.clickable { selected = voice.name; TtsVoicePreferences.selectVoice(context, voice.name) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsPanel(name: String, onDismiss: () -> Unit, onExport: () -> Unit, onRestore: () -> Unit) {
    val context = LocalContext.current
    val readerPrefs = remember { context.getSharedPreferences("reader_preferences", android.content.Context.MODE_PRIVATE) }
    var fontSize by remember { mutableFloatStateOf(readerPrefs.getFloat("font_size", 20f)) }
    var spacing by remember { mutableFloatStateOf(readerPrefs.getFloat("line_spacing", 1.55f)) }
    var continueChapters by remember { mutableStateOf(readerPrefs.getBoolean("continue_chapters", true)) }
    var storageBytes by remember { mutableLongStateOf(context.filesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }) }
    var cacheBytes by remember { mutableLongStateOf(context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            when (name) {
                "Default reader settings" -> {
                    Text("Default text size: ${fontSize.toInt()}")
                    Slider(fontSize, { fontSize = it; readerPrefs.edit().putFloat("font_size", it).apply() }, valueRange = 14f..44f)
                    Text("Default line spacing: ${String.format(Locale.getDefault(), "%.1f", spacing)}")
                    Slider(spacing, { spacing = it; readerPrefs.edit().putFloat("line_spacing", it).apply() }, valueRange = 1.1f..2.1f)
                    Text("These defaults are applied when a reader is opened.")
                }
                "Playback behaviour" -> {
                    ListItem(headlineContent = { Text("Continue to next chapter") }, supportingContent = { Text("Keep reading until the end of the book") }, trailingContent = { Switch(continueChapters, { continueChapters = it; readerPrefs.edit().putBoolean("continue_chapters", it).apply() }) })
                    Text("Speech speed and voice are available in Text-to-speech voice settings.")
                }
                "Storage management" -> {
                    Text("Books and extracted files: ${formatBytes(storageBytes)}")
                    Text("Safe cache: ${formatBytes(cacheBytes)}")
                    Button(onClick = { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }; cacheBytes = 0L }) { Icon(Icons.Default.CleaningServices, null); Spacer(Modifier.width(8.dp)); Text("Clear safe cache") }
                    Text("Books, bookmarks, notes, and reading positions are not removed.")
                }
                "Backup and restore" -> {
                    Text("Export or restore your reader theme, text layout, playback behavior, speech speed, and selected voice. Files stay on this device.")
                    Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(8.dp)); Text("Export settings backup") }
                    OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Restore, null); Spacer(Modifier.width(8.dp)); Text("Restore settings backup") }
                    Text("Book files and reading history are not included in this small settings backup.")
                }
                "Privacy" -> {
                    Text("Read Aloud Library has no account, ads, analytics, tracking, or cloud sync. Imported books, listening choices, bookmarks, and reading history remain on this device.")
                    Text("The app does not request Android's Internet permission.")
                }
                "About" -> {
                    Text("Read Aloud Library")
                    Text("Version 1.0.0")
                    Text("A private native Android reader for EPUB, PDF, TXT, HTML, Markdown, RTF, and FB2 books.")
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1_024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1_024.0)
    else -> "$bytes B"
}
