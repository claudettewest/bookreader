package com.claudettewest.offlinebookshelf.ui

import android.content.Intent
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
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil3.compose.AsyncImage
import com.claudettewest.offlinebookshelf.data.BookEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
            composable("reading") { EmptyState(Icons.Default.MenuBook, "Currently reading", "Open a book from your library to continue.") }
            composable("bookmarks") { EmptyState(Icons.Default.Bookmark, "Bookmarks", "Your saved places will appear here.") }
            composable("settings") { SettingsScreen() }
            composable("reader/{bookId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { ReaderScreen(it.arguments?.getString("bookId")!!, onBack = nav::popBackStack) }
        }
    }
    state.message?.let { message -> Snackbar(Modifier.padding(16.dp), action = { TextButton(vm::clearMessage) { Text("OK") } }) { Text(message) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LibraryScreen(state: LibraryState, vm: LibraryViewModel, onImport: () -> Unit, onOpen: (BookEntity) -> Unit) {
    Scaffold(floatingActionButton = { ExtendedFloatingActionButton(onClick = onImport, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Import book") }) }, topBar = { TopAppBar(title = { Column { Text("Offline Bookshelf", fontWeight = FontWeight.Bold); Text("Your private library", style = MaterialTheme.typography.labelMedium) } }, actions = { IconButton(vm::toggleView) { Icon(if (state.grid) Icons.Default.ViewList else Icons.Default.GridView, "Change library layout") } }) }) { insets ->
        Column(Modifier.padding(insets).fillMaxSize()) {
            OutlinedTextField(state.query, vm::query, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search title or author") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(SortMode.entries) { sort -> FilterChip(state.sort == sort, { vm.sort(sort) }, { Text(sort.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) }) } }
            if (state.importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.books.isEmpty()) EmptyState(Icons.Default.AutoStories, "Your shelf is waiting", "Import EPUB, PDF, TXT, HTML, Markdown, RTF, or FB2 files. Everything stays on this device.")
            else if (state.grid) LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(state.books, key = { it.id }) { BookCard(it, onOpen) } }
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(state.books, key = { it.id }) { BookRow(it, onOpen) } }
        }
    }
}

@Composable private fun BookCover(book: BookEntity, modifier: Modifier = Modifier) { Box(modifier.clip(RoundedCornerShape(12.dp)).background(when (book.title.hashCode().mod(3)) { 0 -> Silver; 1 -> TaupeGray; else -> Charcoal }), contentAlignment = Alignment.Center) { if (book.coverPath != null) AsyncImage(book.coverPath, "Cover of ${book.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AutoStories, null, tint = Platinum); Spacer(Modifier.height(8.dp)); Text(book.title, color = Platinum, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun BookCard(book: BookEntity, onOpen: (BookEntity)->Unit) { Column(Modifier.clickable { onOpen(book) }) { BookCover(book, Modifier.fillMaxWidth().aspectRatio(.68f)); Spacer(Modifier.height(8.dp)); Text(book.title, fontWeight = FontWeight.SemiBold, maxLines = 2); Text(book.author, style = MaterialTheme.typography.bodySmall, maxLines = 1); LinearProgressIndicator({ book.progress }, Modifier.fillMaxWidth().padding(top=6.dp)); Text(book.format.name, style = MaterialTheme.typography.labelSmall) } }
@Composable private fun BookRow(book: BookEntity, onOpen: (BookEntity)->Unit) { Card(onClick = { onOpen(book) }) { Row(Modifier.padding(12.dp)) { BookCover(book, Modifier.width(64.dp).height(92.dp)); Column(Modifier.padding(start=14.dp).weight(1f)) { Text(book.title, fontWeight=FontWeight.Bold); Text(book.author); Spacer(Modifier.weight(1f)); LinearProgressIndicator({book.progress}, Modifier.fillMaxWidth()); Text("${(book.progress*100).toInt()}% • ${book.format.name}", style=MaterialTheme.typography.labelSmall) } } } }

@Composable fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(56.dp), tint=TaupeGray); Spacer(Modifier.height(16.dp)); Text(title, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold); Text(body, Modifier.padding(top=8.dp), style=MaterialTheme.typography.bodyMedium) } } }

@Composable private fun SettingsScreen() { LazyColumn(contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) { item { Text("Settings", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold) }; items(listOf(Icons.Default.TextFields to "Default reader settings", Icons.Default.RecordVoiceOver to "Text-to-speech", Icons.Default.Storage to "Storage management", Icons.Default.Backup to "Backup and restore", Icons.Default.PrivacyTip to "Privacy", Icons.Default.Info to "About")) { (icon, label) -> Card { ListItem(headlineContent={Text(label)}, leadingContent={Icon(icon,null)}, trailingContent={Icon(Icons.Default.ChevronRight,null)}) } }; item { Text("Private by design", fontWeight=FontWeight.Bold); Text("Books, bookmarks, reading history, and listening positions remain on this device. Offline Bookshelf has no account, ads, analytics, tracking, cloud sync, or Internet permission.") } } }
