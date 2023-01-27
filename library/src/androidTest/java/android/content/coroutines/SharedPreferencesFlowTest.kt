package android.content.coroutines

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPreferencesFlowTest : BaseSharedPreferencesTest() {
    private inline fun <reified T: Any> testFlow(
        crossinline getValueFlow: SharedPreferences.(key: String) -> Flow<T?>,
        crossinline updateValue: suspend SharedPreferences.(key: String, value: T) -> Unit,
        crossinline nextValue: () -> T
    ) {
        val clazzName = T::class.simpleName
        runBlocking {
            prefernces.getValueFlow(TEST_KEY1)
                .collect {
                    assertEquals("$clazzName values do not match", it, null)
                }
        }
        repeat(10)  {
            val value = nextValue.invoke()
            runBlocking { prefernces.updateValue(TEST_KEY1, value) }
            runBlocking {
                prefernces.getValueFlow(TEST_KEY1)
                    .collect {
                        assertEquals("$clazzName values do not match", it, value)
                    }
            }
        }
    }

    @Test
    fun test_setAndGetInt() = testFlow<Int>(
        getValueFlow = { getIntFlow(it) },
        updateValue = { key, value -> editCo { putInt(key, value) } },
        nextValue = { random.nextInt() }
    )

    @Test
    fun test_setAndGetLong() = testFlow<Long>(
        getValueFlow = { getLongFlow(it) },
        updateValue = { key, value -> editCo { putLong(key, value) } },
        nextValue = { random.nextLong() }
    )

    @Test
    fun test_setAndGetFloat() = testFlow<Float>(
        getValueFlow = { getFloatFlow(it) },
        updateValue = { key, value -> editCo { putFloat(key, value) } },
        nextValue = { random.nextFloat() }
    )

    @Test
    fun test_setAndGetBoolean() = testFlow<Boolean>(
        getValueFlow = { getBooleanFlow(it) },
        updateValue = { key, value -> editCo { putBoolean(key, value) } },
        nextValue = { random.nextBoolean() }
    )

    @Test
    fun test_setAndGetString() = testFlow<String>(
        getValueFlow = { getStringFlow(it) },
        updateValue = { key, value -> editCo { putString(key, value) } },
        nextValue = { random.nextInt().toString() }
    )

    @Test
    fun test_setAndGetStringSet() = testFlow<Set<String?>>(
        getValueFlow = { getStringSetFlow(it) },
        updateValue = { key, value -> editCo { putStringSet(key, value) } },
        nextValue = {
            val size = 10 + random.nextInt(10)
            val set = HashSet<String>(size)
            repeat(size) {
                set.add(random.nextInt().toString())
            }
            return@testFlow set
        }
    )
}