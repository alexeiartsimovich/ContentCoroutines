package android.content.coroutines

import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val indexCounter = AtomicLong(0)

// We need to keep references on active OnSharedPreferenceChangeListeners,
// because SharedPreferences hold a weak reference to them when registering.
private val sharedPreferenceChangeListeners =
    ConcurrentHashMap<SharedPreferences.OnSharedPreferenceChangeListener, Unit>()

private class OnSharedPreferenceChangeListenerImpl(
    private val index: Long,
    private val onChanged: (key: String?) -> Unit
): SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        onChanged.invoke(key)
    }

    override fun hashCode(): Int = index.toInt()

    override fun equals(other: Any?): Boolean {
        // Compare references only
        return this === other
    }
}

internal object SharedPreferenceChangeListenerPool {
    fun acquire(
        onChanged: (key: String?) -> Unit
    ): SharedPreferences.OnSharedPreferenceChangeListener {
        val index = indexCounter.getAndIncrement()
        val listener = OnSharedPreferenceChangeListenerImpl(index, onChanged)
        sharedPreferenceChangeListeners.put(listener, Unit)
        return listener
    }

    fun release(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferenceChangeListeners.remove(listener)
    }
}