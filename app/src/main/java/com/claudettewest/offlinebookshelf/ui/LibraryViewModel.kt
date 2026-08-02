package com.claudettewest.offlinebookshelf.ui

import android.net.Uri
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.claudettewest.offlinebookshelf.data.*
import com.claudettewest.offlinebookshelf.importer.BookImporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortMode { LAST_OPENED, TITLE, AUTHOR, DATE_ADDED, PROGRESS }
data class LibraryState(val books: List<BookEntity> = emptyList(), val query: String = "", val sort: SortMode = SortMode.LAST_OPENED, val format: BookFormat? = null, val grid: Boolean = true, val importing: Boolean = false, val message: String? = null)

class LibraryViewModel(private val dao: LibraryDao, private val importer: BookImporter) : ViewModel() {
    private val controls = MutableStateFlow(LibraryState())
    val state = combine(dao.observeBooks(), controls) { books, state ->
        val filtered = books.filter { (state.query.isBlank() || it.title.contains(state.query, true) || it.author.contains(state.query, true)) && (state.format == null || it.format == state.format) }
        state.copy(books = when(state.sort) { SortMode.TITLE -> filtered.sortedBy { it.title.lowercase() }; SortMode.AUTHOR -> filtered.sortedBy { it.author.lowercase() }; SortMode.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }; SortMode.PROGRESS -> filtered.sortedByDescending { it.progress }; else -> filtered.sortedByDescending { it.lastOpenedAt ?: it.dateAdded } })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())
    fun query(value: String) { controls.update { it.copy(query = value) } }
    fun toggleView() { controls.update { it.copy(grid = !it.grid) } }
    fun sort(value: SortMode) { controls.update { it.copy(sort = value) } }
    fun import(uris: List<Uri>) = viewModelScope.launch { controls.update { it.copy(importing = true) }; val results = uris.map { importer.import(it) }; val failures = results.filter { it.error != null }; controls.update { it.copy(importing = false, message = if (failures.isEmpty()) "Imported ${results.size} book${if(results.size == 1) "" else "s"}." else "Imported ${results.size - failures.size}; ${failures.size} could not be imported: ${failures.joinToString { it.sourceName }}") } }
    fun clearMessage() { controls.update { it.copy(message = null) } }
    companion object { fun factory(dao: LibraryDao, importer: BookImporter) = viewModelFactory { initializer { LibraryViewModel(dao, importer) } } }
}
