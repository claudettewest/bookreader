package com.claudettewest.offlinebookshelf

import android.app.Application
import androidx.room.Room
import com.claudettewest.offlinebookshelf.data.BookshelfDatabase
import com.claudettewest.offlinebookshelf.importer.BookImporter

class BookshelfApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, BookshelfDatabase::class.java, "offline-bookshelf.db").build() }
    val importer by lazy { BookImporter(this, database.libraryDao()) }
}
