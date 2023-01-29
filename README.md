# ContentCoroutines
A lightweight library with coroutine methods for accessing and modifying content.

## Getting started
If you use Gradle, add this repository in the root build file:
```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
Then add this dependency in a project build file:
```
dependencies {
  implementation 'com.github.alexeiartsimovich:ContentCoroutines:v1.0.0'
}
```

## ContentCoroutines example
Here is an example of how you can observe playlists from MediaStore:
```
GlobalScope.launch {
    val uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
    val projection: Array<String> = arrayOf(
        MediaStore.Audio.Playlists._ID,
        MediaStore.Audio.Playlists.NAME
    )
    val cursorMapper = CursorMapper<Playlist> { cursor ->
        Playlist(
            id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)),
            name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME))
        )
    }
    val flow = contentResolver.queryWithFlow(
        uri = uri,
        projection = projection,
        mapper = cursorMapper
    )
    flow.collect { list ->
        // Here you get the latest version of playlists from MediaStore
    }
}
```

Another example of how you can observe values from SharedPreferences:
```
GlobalScope.launch {
    val preferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
    preferences.getStringValueFlow("my_key")
        .collect { value ->
            // Here you get the latest value for your key from the shared preferences
        }
}
```
