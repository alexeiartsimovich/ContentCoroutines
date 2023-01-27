package android.content.coroutines

import android.content.Context
import android.content.SharedPreferences
import androidx.test.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test
    fun test_setAndGetInt() = runBlocking {
        val value = random.nextInt()
        prefernces.editCo { putInt(TEST_KEY1, value) }
        assertEquals("Int values are not equal", value, prefernces.getIntCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetLong() = runBlocking {
        val value = random.nextLong()
        prefernces.editCo { putLong(TEST_KEY1, value) }
        assertEquals("Long values are not equal", value, prefernces.getLongCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetFloat() = runBlocking {
        val value = random.nextFloat()
        prefernces.editCo { putFloat(TEST_KEY1, value) }
        assertEquals("Float values are not equal", value, prefernces.getFloatCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetString() = runBlocking {
        val value = random.nextInt().toString()
        prefernces.editCo { putString(TEST_KEY1, value) }
        assertEquals("String values are not equal", value, prefernces.getStringCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetStringSet() = runBlocking {
        val value: Set<String?> = HashSet<String>().apply {
            repeat(10) { add(random.nextInt().toString()) }
        }
        prefernces.editCo { putStringSet(TEST_KEY1, value) }
        val restored = prefernces.getStringSetCo(TEST_KEY1)!!
        assertEquals("String set values are not equal", value,
            value.containsAll(restored) && restored.containsAll(value)
        )
    }

    @After
    fun tearDown() {
        prefernces.edit().clear().commit()
    }

    companion object {
        private const val TEST_KEY1 = "test_key1"
    }
}