package android.content.coroutines

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine


//region editor
suspend fun SharedPreferences.editCo(block: SharedPreferences.Editor.() -> Unit) {
    suspendCoroutine<Unit> { continuation ->
        ExecutorProvider.io.execute {
            continuation.resumeWith(
                runCatching<Unit> { edit().apply(block).commit() }
            )
        }
    }
}
//endregion

//region suspended functions
suspend fun SharedPreferences.getStringCo(key: String, defValue: String? = null): String? {
    return getValueCo { getString(key, defValue) }
}

suspend fun SharedPreferences.getIntCo(key: String, defValue: Int? = null): Int? {
    return getValueCo { if (contains(key)) getInt(key, defValue ?: 0) else null }
}

suspend fun SharedPreferences.getLongCo(key: String, defValue: Long? = null): Long? {
    return getValueCo { if (contains(key)) getLong(key, defValue ?: 0L) else null }
}

suspend fun SharedPreferences.getFloatCo(key: String, defValue: Float? = null): Float? {
    return getValueCo { if (contains(key)) getFloat(key, defValue ?: 0f) else null }
}

suspend fun SharedPreferences.getStringSetCo(key: String, defValues: Set<String?>? = null): Set<String?>? {
    return getValueCo { getStringSet(key, defValues) }
}

suspend fun SharedPreferences.getBooleanCo(key: String, defValue: Boolean? = null): Boolean? {
    return getValueCo { if (contains(key)) getBoolean(key, defValue ?: false) else null }
}

suspend fun <V> SharedPreferences.getValueCo(getter: SharedPreferences.() -> V): V? {
    return suspendCoroutine<V?> { continuation ->
        ExecutorProvider.io.execute {
            continuation.resumeWith(
                runCatching<V?> { getter() }
            )
        }
    }
}
//endregion

//region Flows
private fun <T> SharedPreferences.getValueFlow(
    key: String,
    get: suspend SharedPreferences.() -> T
): Flow<T?> {
    return callbackFlow {
        val listener = SharedPreferenceChangeListenerPool.acquire { changedKey ->
            if (changedKey == key) {
                launch {
                    trySend(this@getValueFlow.get())
                }
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        trySend(this@getValueFlow.get())
        awaitClose {
            unregisterOnSharedPreferenceChangeListener(listener)
            SharedPreferenceChangeListenerPool.release(listener)
        }
    }
}

fun SharedPreferences.getStringFlow(key: String, defValue: String? = null): Flow<String?> {
    return getValueFlow(key = key) { getStringCo(key) ?: defValue }
}

fun SharedPreferences.getIntFlow(key: String, defValue: Int? = null): Flow<Int?> {
    return getValueFlow(key = key) { getIntCo(key, defValue) }
}

fun SharedPreferences.getLongFlow(key: String, defValue: Long? = null): Flow<Long?> {
    return getValueFlow(key = key) { getLongCo(key, defValue) }
}

fun SharedPreferences.getFloatFlow(key: String, defValue: Float? = null): Flow<Float?> {
    return getValueFlow(key = key) { getFloatCo(key, defValue) }
}

fun SharedPreferences.getStringSetFlow(key: String, defValue: Set<String?>? = null): Flow<Set<String?>?> {
    return getValueFlow(key = key) { getStringSet(key, defValue) }
}

fun SharedPreferences.getBooleanFlow(key: String, defValue: Boolean? = null): Flow<Boolean?> {
    return getValueFlow(key = key) { getBooleanCo(key, defValue) }
}
//endregion