package android.content.coroutines

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private val sharedObserverHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

fun <T> ContentResolver.queryByIdWithFlow(
    uri: Uri,
    itemId: Long,
    projection: Array<String>? = null,
    mapper: CursorMapper<T>
): Flow<T?> {
    val itemUri = ContentUris.withAppendedId(uri, itemId)
    return queryWithFlow(itemUri, projection, selection = null, selectionArgs = null, sortOrder = null, mapper)
        .map { list ->
            if (list.size > 1) {
                throw IllegalArgumentException("More than 1 item found: uri=$uri, id=$itemId")
            }
            list.firstOrNull()
        }
}

fun <T> ContentResolver.queryWithFlow(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String?>? = null,
    sortOrder: String? = null,
    mapper: CursorMapper<T>
): Flow<List<T>> {
    return callbackFlow {
        val observer = object : ContentObserver(sharedObserverHandler) {
            override fun onChange(selfChange: Boolean) {
                launch {
                    trySend(queryListCo(uri, projection, selection, selectionArgs, sortOrder, mapper))
                }
            }
        }
        registerContentObserver(uri, false, observer)
        trySend(queryListCo(uri, projection, selection, selectionArgs, sortOrder, mapper))
        awaitClose {
            unregisterContentObserver(observer)
        }
    }
}

private suspend fun <T> ContentResolver.queryListCo(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String?>? = null,
    sortOrder: String? = null,
    mapper: CursorMapper<T>
): List<T> {
    return suspendCancellableCoroutine<List<T>> { continuation ->
        ExecutorProvider.io.execute {
            if (continuation.isCancelled) {
                return@execute
            }
            val result = kotlin.runCatching {
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation { cancellationSignal.cancel() }
                val cursor =
                    query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal)
                        ?: throw NullPointerException("No result for query: uri=$uri")
                val resultList = ArrayList<T>(cursor.count)
                cursor.use { _cursor ->
                    if (_cursor.moveToFirst()) {
                        do {
                            val item = mapper.mapCursor(_cursor)
                            resultList.add(item)
                        } while (_cursor.moveToNext())
                    }
                }
                return@runCatching resultList
            }
            continuation.resumeWith(result)
        }
    }
}