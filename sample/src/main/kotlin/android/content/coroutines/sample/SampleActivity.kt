package android.content.coroutines.sample

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.coroutines.*
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.random.Random


class SampleActivity : AppCompatActivity() {
    private val random by lazy { Random(System.currentTimeMillis()) }
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<Playlist>

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        runOnUiThread {
            Toast.makeText(this, "Error: $exception", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        listView = findViewById(R.id.list_view)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        adapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                listView.setSelection(adapter.count - 1)
            }
        })
        listView.adapter = adapter
        findViewById<Button>(R.id.clear_button)?.setOnClickListener {
            clearPlaylists()
        }
        findViewById<Button>(R.id.add_button)?.setOnClickListener {
            addNewPlaylist()
        }
        findViewById<Button>(R.id.request_permissions_button)?.setOnClickListener {
            requestPermissions(PERMISSIONS, RC_REQUEST_PERMISSIONS)
        }
        ContentCoroutines.setIoExecutor(
            Executors.newSingleThreadExecutor {
                Thread(it, "ContentCoroutinesSampleThread")
            }
        )
        lifecycleScope.launch(exceptionHandler) {
            val flow = contentResolver.queryWithFlow(uri = URI,
                projection = PROJECTION, mapper = CURSOR_MAPPER)
            flow.collect { list ->
                adapter.clear()
                adapter.addAll(list)
            }
        }
    }

    private fun clearPlaylists() = lifecycleScope.launch(exceptionHandler) {
        val count = contentResolver.deleteRows(URI, null, null)
        contentResolver.notifyChange(URI, null)
        Toast.makeText(this@SampleActivity,
            "Deleted $count playlist(s)", Toast.LENGTH_SHORT).show()
    }

    private fun addNewPlaylist() = lifecycleScope.launch(exceptionHandler) {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val nameBuilder = StringBuilder()
        repeat(5 + random.nextInt(10)) {
            val char = allowedChars.random()
            nameBuilder.append(char)
        }
        val name = nameBuilder.toString()
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Playlists.NAME, name)
        }
        val uri = contentResolver.insertRow(URI, contentValues)!!
        val playlist = Playlist(
            id = ContentUris.parseId(uri),
            name = name
        )
        Toast.makeText(this@SampleActivity,
            "Added new item: $playlist", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val URI: Uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
        private val PROJECTION: Array<String> = arrayOf(
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
        )

        @SuppressLint("Range")
        private val CURSOR_MAPPER = CursorMapper<Playlist> { cursor ->
            Playlist(
                id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)),
                name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME))
            )
        }

        private const val RC_REQUEST_PERMISSIONS = 1337
        private val PERMISSIONS = arrayOf<String>(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
}