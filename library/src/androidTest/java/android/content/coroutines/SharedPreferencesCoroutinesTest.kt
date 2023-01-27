package android.content.coroutines

import android.content.Context
import android.content.SharedPreferences
import androidx.test.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class SharedPreferencesCoroutinesTest {
    private lateinit var prefernces: SharedPreferences
    private lateinit var random: Random

    @Before
    fun setUp() {
        prefernces = InstrumentationRegistry.getContext()
            .getSharedPreferences("test", Context.MODE_PRIVATE)
        random = Random(System.currentTimeMillis())
    }

    private fun testCo(block: suspend CoroutineScope.() -> Unit) {
        runBlocking(block = block)
    }

    @Test
    fun test_setAndGetInt() = testCo {
        val value = random.nextInt()
        prefernces.editCo { putInt(TEST_KEY1, value) }
        assertEquals("Int values are not equal", value, prefernces.getIntCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetLong() = testCo {
        val value = random.nextLong()
        prefernces.editCo { putLong(TEST_KEY1, value) }
        assertEquals("Long values are not equal", value, prefernces.getLongCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetFloat() = testCo {
        val value = random.nextFloat()
        prefernces.editCo { putFloat(TEST_KEY1, value) }
        assertEquals("Float values are not equal", value, prefernces.getFloatCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetString() = testCo {
        val value = random.nextInt().toString()
        prefernces.editCo { putString(TEST_KEY1, value) }
        assertEquals("String values are not equal", value, prefernces.getStringCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetStringSet() = testCo {
        val value: Set<String?> = HashSet<String>().apply {
            repeat(10) { add(random.nextInt().toString()) }
        }
        prefernces.editCo { putStringSet(TEST_KEY1, value) }
        val restored = prefernces.getStringSetCo(TEST_KEY1)!!
        assertTrue("String set values are not equal",
            value.containsAll(restored) && restored.containsAll(value)
        )
    }

    @Test
    fun test_setAndGetBoolean() = testCo {
        val value = random.nextBoolean()
        prefernces.editCo { putBoolean(TEST_KEY1, value) }
        assertEquals("Boolean values are not equal", value, prefernces.getBooleanCo(TEST_KEY1))
    }

    @After
    fun tearDown() {
        prefernces.edit().clear().commit()
    }

    companion object {
        private const val TEST_KEY1 = "test_key1"
    }
}