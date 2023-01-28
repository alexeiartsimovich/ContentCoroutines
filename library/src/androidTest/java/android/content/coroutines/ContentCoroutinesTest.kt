package android.content.coroutines

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.test.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentCoroutinesTest {
    private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        contentResolver = InstrumentationRegistry.getContext().contentResolver
        contentResolver.delete(URI, null, null)
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

    @Test
    fun test_queryWithFlow() {
        val flow = contentResolver.queryWithFlow(URI, PROJECTION,
            null, null, null, CURSOR_MAPPER)
        runBlocking {
            assertTrue("Returned list is not empty", flow.first().isEmpty())
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