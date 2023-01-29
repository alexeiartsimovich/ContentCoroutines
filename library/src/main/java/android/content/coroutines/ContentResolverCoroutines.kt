package android.content.coroutines

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.ContentObserver
import android.net.Uri
import android.os.*
import androidx.annotation.RequiresApi
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
                throw IllegalArgumentException("Query returned more than one element: uri=$itemUri")
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
                    trySend(query(uri, projection, selection, selectionArgs, sortOrder, mapper))
                }
            }
        }
        registerContentObserver(uri, false, observer)
        trySend(query(uri, projection, selection, selectionArgs, sortOrder, mapper))
        awaitClose {
            unregisterContentObserver(observer)
        }
    }
}

suspend fun <T> ContentResolver.query(
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
                        ?: throw NullPointerException("Query returned null: uri=$uri")
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

suspend fun ContentResolver.insertRow(
    uri: Uri,
    values: ContentValues
): Uri? = suspendImpl { insert(uri, values) }

@RequiresApi(Build.VERSION_CODES.R)
suspend fun ContentResolver.insertRow(
    uri: Uri,
    values: ContentValues,
    extras: Bundle? = null
): Uri? = suspendImpl { insert(uri, values, extras) }

suspend fun ContentResolver.updateRows(
    uri: Uri,
    values: ContentValues,
    where: String? = null,
    selectionArgs: Array<String?>? = null
): Int = suspendImpl { update(uri, values, where, selectionArgs) }

@RequiresApi(Build.VERSION_CODES.R)
suspend fun ContentResolver.updateRows(
    uri: Uri,
    values: ContentValues,
    extras: Bundle? = null
): Int = suspendImpl { update(uri, values, extras) }

suspend fun ContentResolver.deleteRows(
    uri: Uri,
    where: String? = null,
    selectionArgs: Array<String?>? = null
): Int = suspendImpl { delete(uri, where, selectionArgs) }

@RequiresApi(Build.VERSION_CODES.R)
suspend fun ContentResolver.deleteRows(
    uri: Uri,
    extras: Bundle? = null
): Int = suspendImpl { delete(uri, extras) }

private suspend fun <T> suspendImpl(lambda: () -> T): T {
    return suspendCancellableCoroutine<T> { continuation ->
        ExecutorProvider.io.execute {
            val result: Result<T> = runCatching { lambda.invoke() }
            continuation.resumeWith(result)
        }
    }
}
