package com.claudettewest.offlinebookshelf.importer

import com.claudettewest.offlinebookshelf.data.ChapterEntity
import org.jsoup.Jsoup
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

class EpubParser {
    private data class ManifestItem(val href: String, val mediaType: String, val properties: String)

    fun parse(file: File, root: File, bookId: String): ParsedBook = ZipFile(file).use { zip ->
        fun xml(path: String) = zip.getInputStream(zip.getEntry(path) ?: error("EPUB is missing $path")).use { DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(it) }
        val container = xml("META-INF/container.xml")
        val opfPath = container.getElementsByTagNameNS("*", "rootfile").item(0).attributes.getNamedItem("full-path").nodeValue
        val opf = xml(opfPath); val base = opfPath.substringBeforeLast('/', "")
        fun meta(tag: String) = opf.getElementsByTagNameNS("*", tag).item(0)?.textContent?.trim().orEmpty()
        val manifest = mutableMapOf<String, ManifestItem>()
        val items = opf.getElementsByTagNameNS("*", "item")
        for (i in 0 until items.length) {
            val a = items.item(i).attributes
            manifest[a.getNamedItem("id").nodeValue] = ManifestItem(a.getNamedItem("href").nodeValue, a.getNamedItem("media-type").nodeValue, a.getNamedItem("properties")?.nodeValue.orEmpty())
        }
        val resources = File(root, "images").apply { mkdirs() }
        manifest.values.filter { it.mediaType.startsWith("image/") }.forEach { item ->
            val href = item.href
            val path = resolve(base, href); zip.getEntry(path)?.let { entry -> File(resources, href.substringAfterLast('/')).apply { parentFile?.mkdirs(); zip.getInputStream(entry).use { input -> outputStream().use(input::copyTo) } } }
        }
        val tocTitles = mutableMapOf<String, String>()
        manifest.values.firstOrNull { "nav" in it.properties.split(' ') }?.let { nav ->
            zip.getEntry(resolve(base, nav.href))?.let { entry ->
                val document = zip.getInputStream(entry).bufferedReader().use { Jsoup.parse(it.readText()) }
                document.select("nav a").forEach { link -> tocTitles[link.attr("href").substringBefore('#').substringAfterLast('/')] = link.text().trim() }
            }
        }
        val spine = opf.getElementsByTagNameNS("*", "itemref")
        val chaptersDir = File(root, "chapters").apply { mkdirs() }
        val chapters = (0 until spine.length).mapNotNull { index ->
            val idref = spine.item(index).attributes.getNamedItem("idref").nodeValue
            val href = manifest[idref]?.href ?: return@mapNotNull null
            val entry = zip.getEntry(resolve(base, href)) ?: return@mapNotNull null
            val html = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            val document = Jsoup.parse(html)
            val title = tocTitles[href.substringAfterLast('/')] ?: document.selectFirst("h1,h2,title")?.text()?.ifBlank { null } ?: "Chapter ${index + 1}"
            document.select("img").forEach { img -> img.attr("src", File(resources, img.attr("src").substringAfterLast('/')).absolutePath) }
            document.select("image").forEach { image ->
                val ref = image.attr("href").ifBlank { image.attr("xlink:href") }
                val local = File(resources, ref.substringAfterLast('/')).absolutePath
                image.attr("href", local).attr("xlink:href", local)
            }
            val output = File(chaptersDir, "$index.html").apply { writeText(document.body().html()) }
            val searchableText = document.select("h1,h2,h3,h4,p,li,blockquote").map { it.text().trim() }.filter(String::isNotBlank).joinToString("\n\n").ifBlank { document.text() }
            ChapterEntity(bookId, index, idref, title, output.absolutePath, searchableText)
        }
        val coverHref = manifest.entries.firstOrNull { (_, item) -> item.mediaType.startsWith("image/") && ("cover-image" in item.properties || item.href.contains("cover", true)) }?.value?.href
        val cover = coverHref?.let { File(resources, it.substringAfterLast('/')).takeIf(File::exists)?.absolutePath }
        ParsedBook(meta("title"), meta("creator"), chapters, cover)
    }

    private fun resolve(base: String, href: String): String = listOf(base, href.substringBefore('#')).filter { it.isNotBlank() }.joinToString("/").split('/').fold(mutableListOf<String>()) { parts, item ->
        when (item) { ".", "" -> Unit; ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex); else -> parts += item }
        parts
    }.joinToString("/")
}
