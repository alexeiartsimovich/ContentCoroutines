package android.content.coroutines

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.test.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Collections
import kotlin.random.Random

class ContentCoroutinesTest {
    private lateinit var contentResolver: ContentResolver
    private lateinit var random: Random

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Before
    fun setUp() {
        contentResolver = InstrumentationRegistry.getContext().contentResolver
        contentResolver.delete(URI, null, null)
        random = Random(System.currentTimeMillis())
    }

    private fun <T> List<T>.compareContents(other: List<T>): Boolean {
        if (this.size != other.size) {
            return false
        }
        for (i in other.indices) {
            if (this[i] != other[i]) {
                return false
            }
        }
        return true
    }

    private fun generatePlaylistName(): String {
        return random.nextInt().toString()
    }

    private fun createPlaylistContentValues(name: String): ContentValues {
        return ContentValues().apply { put(MediaStore.Audio.Playlists.NAME, name) }
    }

    @Test
    fun test_queryWithFlow() {
        val flow = contentResolver.queryWithFlow(URI, PROJECTION,
            null, null, null, CURSOR_MAPPER)
        runBlocking {
            assertTrue("Returned list is not empty", flow.first().isEmpty())
        }
        val list = Collections.synchronizedList(ArrayList<Playlist>(10))
        repeat(10) {
            runBlocking {
                val name = generatePlaylistName()
                val uri = contentResolver.insert(URI, createPlaylistContentValues(name))!!
                val id = ContentUris.parseId(uri)
                val playlist = Playlist(id, name)
                list.add(playlist)
                val returnedList = flow.last()
                assertTrue("Playlist lists do not match", list.compareContents(returnedList))
            }
        }
    }

    @Test
    fun test_queryByIdWithFlow() {
        repeat(10) {
            val flow = contentResolver.queryByIdWithFlow(URI, 0, PROJECTION, CURSOR_MAPPER)
            runBlocking {
                assertTrue("A non-null item was returned", flow.firstOrNull() == null)
            }
        }

        val list = Collections.synchronizedList(ArrayList<Playlist>(10))
        repeat(10) {
            val name = generatePlaylistName()
            val uri = contentResolver.insert(URI, createPlaylistContentValues(name))!!
            val id = ContentUris.parseId(uri)
            val playlist = Playlist(id, name)
            list.add(playlist)
        }
        repeat(10) { index ->
            runBlocking {
                val item = list[index]
                val flow = contentResolver.queryByIdWithFlow(URI, item.id, PROJECTION, CURSOR_MAPPER)
                val returnedItem = flow.first()
                assertEquals("Items are not equal", item, returnedItem)
            }
        }
    }

    private data class Playlist(
        val id: Long,
        val name: String
    )

    private companion object {
        private val URI: Uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
        private val PROJECTION: Array<String> = arrayOf(
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
        )

        private val CURSOR_MAPPER = CursorMapper<Playlist> { cursor ->
            Playlist(
                id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)),
                name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME))
            )
        }
    }
}