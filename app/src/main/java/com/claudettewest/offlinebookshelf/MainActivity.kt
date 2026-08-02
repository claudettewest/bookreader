package com.claudettewest.offlinebookshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudettewest.offlinebookshelf.ui.BookshelfApp
import com.claudettewest.offlinebookshelf.ui.BookshelfTheme
import com.claudettewest.offlinebookshelf.ui.LibraryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BookshelfApplication
        setContent { BookshelfTheme { BookshelfApp(viewModel(factory = LibraryViewModel.factory(app.database.libraryDao(), app.importer))) } }
    }
}
