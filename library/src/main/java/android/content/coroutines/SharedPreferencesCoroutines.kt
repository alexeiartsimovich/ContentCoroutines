package android.content.coroutines

import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine


//region editor
/**
 * Suspendable version of the [SharedPreferences.edit] method, the only difference is that
 * you need to edit data in the [block] lambda.
 * If [commit] is true, then the changes will be saved using the [SharedPreferences.Editor.commit] method,
 * otherwise the [SharedPreferences.Editor.apply] will be used.
 */
@SuppressLint("ApplySharedPref")
suspend fun SharedPreferences.edit(commit: Boolean = false, block: SharedPreferences.Editor.() -> Unit) {
    suspendCoroutine<Unit> { continuation ->
        ExecutorProvider.io.execute {
            continuation.resumeWith(
                runCatching<Unit> {
                    val editor = edit()
                    editor.block()
                    if (commit) {
                        editor.commit()
                    } else {
                        editor.apply()
                    }
                }
            )
        }
    }
}
//endregion

//region suspended functions
/**
 * Suspendable version of the [SharedPreferences.getString] method.
 */
suspend fun SharedPreferences.getStringValue(key: String, defValue: String? = null): String? {
    return getValue { getString(key, defValue) }
}

/**
 * Suspendable version of the [SharedPreferences.getInt] method.
 */
suspend fun SharedPreferences.getIntValue(key: String, defValue: Int? = null): Int? {
    return getValue { if (contains(key)) getInt(key, defValue ?: 0) else null }
}

/**
 * Suspendable version of the [SharedPreferences.getLong] method.
 */
suspend fun SharedPreferences.getLongValue(key: String, defValue: Long? = null): Long? {
    return getValue { if (contains(key)) getLong(key, defValue ?: 0L) else null }
}

/**
 * Suspendable version of the [SharedPreferences.getFloat] method.
 */
suspend fun SharedPreferences.getFloatValue(key: String, defValue: Float? = null): Float? {
    return getValue { if (contains(key)) getFloat(key, defValue ?: 0f) else null }
}

/**
 * Suspendable version of the [SharedPreferences.getStringSet] method.
 */
suspend fun SharedPreferences.getStringSetValue(key: String, defValues: Set<String?>? = null): Set<String?>? {
    return getValue { getStringSet(key, defValues) }
}

/**
 * Suspendable version of the [SharedPreferences.getBoolean] method.
 */
suspend fun SharedPreferences.getBooleanValue(key: String, defValue: Boolean? = null): Boolean? {
    return getValue { if (contains(key)) getBoolean(key, defValue ?: false) else null }
}

private suspend fun <V> SharedPreferences.getValue(get: SharedPreferences.() -> V): V? {
    return suspendCoroutine<V?> { continuation ->
        ExecutorProvider.io.execute {
            continuation.resumeWith(
                runCatching<V?> { get() }
            )
        }
    }
}
//endregion

//region Flows
/**
 * Returns a flow of [String] values for the given [key]. Emits a value every time the key is edited.
 */
fun SharedPreferences.getStringValueFlow(key: String, defValue: String? = null): Flow<String?> {
    return getValueFlow(key = key) { getStringValue(key) ?: defValue }
}

/**
 * Returns a flow of [Int] values for the given [key]. Emits a value every time the key is edited.
 */
fun SharedPreferences.getIntValueFlow(key: String, defValue: Int? = null): Flow<Int?> {
    return getValueFlow(key = key) { getIntValue(key, defValue) }
}

/**
 * Returns a flow of [Long] values for the given [key]. Emits a value every time the key is edited.
 */
fun SharedPreferences.getLongValueFlow(key: String, defValue: Long? = null): Flow<Long?> {
    return getValueFlow(key = key) { getLongValue(key, defValue) }
}

/**
 * Returns a flow of [Float] values for the given [key]. Emits a value every time the key is edited.
 */
fun SharedPreferences.getFloatValueFlow(key: String, defValue: Float? = null): Flow<Float?> {
    return getValueFlow(key = key) { getFloatValue(key, defValue) }
}

/**
 * Returns a flow of [Set] of [String] values for the given [key]. Emits a value every time the key is edited.
 */
fun SharedPreferences.getStringSetValueFlow(key: String, defValue: Set<String?>? = null): Flow<Set<String?>?> {
    return getValueFlow(key = key) { getStringSet(key, defValue) }
}

/**
 * Returns a flow of [Boolean] values for the given [key]. Emits a value every time the key is edited.
 */
fun SharedPreferences.getBooleanValueFlow(key: String, defValue: Boolean? = null): Flow<Boolean?> {
    return getValueFlow(key = key) { getBooleanValue(key, defValue) }
}

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
//endregion