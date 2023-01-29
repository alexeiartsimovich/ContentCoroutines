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

/**
 * Returns a flow of models of type [T]: queries data for the given parameters
 * and emits a model every time the data changes.
 * Unlike the [queryWithFlow] method, this method queries only one element by its [elementId].
 */
fun <T> ContentResolver.queryByIdWithFlow(
    uri: Uri,
    elementId: Long,
    projection: Array<String>? = null,
    mapper: CursorMapper<T>
): Flow<T?> {
    val elementUri = ContentUris.withAppendedId(uri, elementId)
    return queryWithFlow(elementUri, projection, selection = null, selectionArgs = null, sortOrder = null, mapper)
        .map { list ->
            if (list.size > 1) {
                throw IllegalArgumentException("Query returned more than one element: uri=$elementUri")
            }
            list.firstOrNull()
        }
}

/**
 * Returns a list flow of models of type [T]: queries data for the given parameters
 * and emits a list every time the data changes.
 * The parameters have the same meaning as in the [ContentResolver.query] method.
 */
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

/**
 * Suspendable version of the [ContentResolver.query] method. Returns a list of models of type [T].
 * Models are mapped using the given [mapper].
 * Throws a [NullPointerException] exception if the query to [uri] returns null.
 */
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

/**
 * Suspendable version of the [ContentResolver.insert] method.
 */
suspend fun ContentResolver.insertRow(
    uri: Uri,
    values: ContentValues
): Uri? = suspendImpl { insert(uri, values) }

/**
 * Suspendable version of the [ContentResolver.insert] method.
 */
@RequiresApi(Build.VERSION_CODES.R)
suspend fun ContentResolver.insertRow(
    uri: Uri,
    values: ContentValues,
    extras: Bundle? = null
): Uri? = suspendImpl { insert(uri, values, extras) }

/**
 * Suspendable version of the [ContentResolver.update] method.
 */
suspend fun ContentResolver.updateRows(
    uri: Uri,
    values: ContentValues,
    where: String? = null,
    selectionArgs: Array<String?>? = null
): Int = suspendImpl { update(uri, values, where, selectionArgs) }

/**
 * Suspendable version of the [ContentResolver.update] method.
 */
@RequiresApi(Build.VERSION_CODES.R)
suspend fun ContentResolver.updateRows(
    uri: Uri,
    values: ContentValues,
    extras: Bundle? = null
): Int = suspendImpl { update(uri, values, extras) }

/**
 * Suspendable version of the [ContentResolver.delete] method.
 */
suspend fun ContentResolver.deleteRows(
    uri: Uri,
    where: String? = null,
    selectionArgs: Array<String?>? = null
): Int = suspendImpl { delete(uri, where, selectionArgs) }

/**
 * Suspendable version of the [ContentResolver.delete] method.
 */
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
