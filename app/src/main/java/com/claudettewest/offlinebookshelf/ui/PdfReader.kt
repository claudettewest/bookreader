package com.claudettewest.offlinebookshelf.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

private class PdfDocument(path: String) : AutoCloseable {
    private val descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)
    val pageCount get() = renderer.pageCount
    override fun close() { renderer.close(); descriptor.close() }
}

@Composable
fun NativePdfReader(path: String, requestedPage: Int, onPageChanged: (Int) -> Unit, onPageCount: (Int) -> Unit, modifier: Modifier = Modifier) {
    val document = remember(path) { PdfDocument(path) }
    DisposableEffect(document) { onDispose { document.close() } }
    var zoom by remember { mutableFloatStateOf(1f) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = requestedPage)
    val widthPx = (LocalConfiguration.current.screenWidthDp * 2.5f).toInt().coerceAtLeast(900)
    LaunchedEffect(document) { onPageCount(document.pageCount) }
    LaunchedEffect(requestedPage) {
        if (listState.firstVisibleItemIndex != requestedPage) listState.animateScrollToItem(requestedPage)
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { onPageChanged(it) }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { _, _, amount, _ -> zoom = (zoom * amount).coerceIn(1f, 3f) }
            },
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items((0 until document.pageCount).toList(), key = { it }) { pageIndex ->
                PdfPage(document, pageIndex, widthPx, zoom)
            }
        }
        Surface(Modifier.align(Alignment.TopEnd).padding(16.dp), shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
            Text("${document.pageCount} pages", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PdfPage(document: PdfDocument, pageIndex: Int, targetWidth: Int, zoom: Float) {
    val bitmap by produceState<Bitmap?>(null, document, pageIndex, targetWidth, zoom) {
        value = withContext(Dispatchers.IO) {
            synchronized(document.renderer) {
                document.renderer.openPage(pageIndex).use { page ->
                    val width = (targetWidth * zoom).toInt().coerceAtMost(3000)
                    val height = (width * page.height.toFloat() / page.width).toInt()
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
                        output.eraseColor(Color.WHITE)
                        page.render(output, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        if (bitmap == null) Box(Modifier.fillMaxWidth().height(500.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Image(bitmap!!.asImageBitmap(), "Page ${pageIndex + 1}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
    }
}
