package com.claudettewest.offlinebookshelf.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class BookFormat { EPUB, PDF, TXT, HTML, MARKDOWN, RTF, FB2 }

@Entity(tableName = "books", indices = [Index("contentHash", unique = true), Index("lastOpenedAt"), Index("title")])
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String = "Unknown author",
    val description: String? = null,
    val publisher: String? = null,
    val language: String? = null,
    val format: BookFormat,
    val sourceUri: String,
    val sourceName: String,
    val privatePath: String,
    val coverPath: String? = null,
    val contentHash: String,
    val fileSize: Long,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
    val progress: Float = 0f,
    val chapterCount: Int = 0,
    val searchable: Boolean = true,
    val ttsAvailable: Boolean = true,
    val ttsPaused: Boolean = false,
)

@Entity(tableName = "chapters", primaryKeys = ["bookId", "ordinal"], indices = [Index("bookId")])
data class ChapterEntity(val bookId: String, val ordinal: Int, val stableId: String, val title: String, val contentPath: String, val plainText: String)

@Entity(tableName = "positions")
data class ReadingPositionEntity(@PrimaryKey val bookId: String, val chapter: Int = 0, val elementId: String? = null, val characterOffset: Int = 0, val scrollOffset: Int = 0, val page: Int = 0, val progress: Float = 0f, val updatedAt: Long = System.currentTimeMillis())

@Entity(tableName = "tts_positions")
data class TtsPositionEntity(@PrimaryKey val bookId: String, val chapter: Int = 0, val paragraph: Int = 0, val sentence: Int = 0, val characterOffset: Int = 0, val updatedAt: Long = System.currentTimeMillis())

@Entity(tableName = "bookmarks", indices = [Index("bookId")])
data class BookmarkEntity(@PrimaryKey val id: String, val bookId: String, val chapter: Int? = null, val page: Int? = null, val elementId: String? = null, val characterOffset: Int = 0, val preview: String = "", val note: String = "", val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "preferences")
data class PreferenceEntity(@PrimaryKey val key: String, val value: String)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM books ORDER BY lastOpenedAt DESC, dateAdded DESC") fun observeBooks(): Flow<List<BookEntity>>
    @Query("SELECT * FROM books WHERE id=:id") fun observeBook(id: String): Flow<BookEntity?>
    @Query("SELECT * FROM books WHERE id=:id") suspend fun book(id: String): BookEntity?
    @Query("SELECT * FROM books WHERE contentHash=:hash LIMIT 1") suspend fun byHash(hash: String): BookEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertBook(book: BookEntity)
    @Update suspend fun updateBook(book: BookEntity)
    @Delete suspend fun deleteBook(book: BookEntity)
    @Query("SELECT * FROM chapters WHERE bookId=:bookId ORDER BY ordinal") fun observeChapters(bookId: String): Flow<List<ChapterEntity>>
    @Query("SELECT * FROM chapters WHERE bookId=:bookId ORDER BY ordinal") suspend fun chapters(bookId: String): List<ChapterEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertChapters(chapters: List<ChapterEntity>)
    @Query("SELECT * FROM positions WHERE bookId=:bookId") fun observePosition(bookId: String): Flow<ReadingPositionEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePosition(position: ReadingPositionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTtsPosition(position: TtsPositionEntity)
    @Query("SELECT * FROM bookmarks WHERE bookId=:bookId ORDER BY createdAt DESC") fun observeBookmarks(bookId: String): Flow<List<BookmarkEntity>>
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC") fun observeAllBookmarks(): Flow<List<BookmarkEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBookmark(bookmark: BookmarkEntity)
    @Delete suspend fun deleteBookmark(bookmark: BookmarkEntity)
    @Query("SELECT * FROM chapters WHERE bookId=:bookId AND plainText LIKE '%' || :query || '%' ORDER BY ordinal") suspend fun search(bookId: String, query: String): List<ChapterEntity>
}

@Database(entities = [BookEntity::class, ChapterEntity::class, ReadingPositionEntity::class, TtsPositionEntity::class, BookmarkEntity::class, PreferenceEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class BookshelfDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}

class Converters {
    @TypeConverter fun format(value: String) = BookFormat.valueOf(value)
    @TypeConverter fun format(value: BookFormat) = value.name
}
