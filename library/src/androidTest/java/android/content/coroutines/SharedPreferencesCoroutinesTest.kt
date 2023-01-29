package android.content.coroutines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesCoroutinesTest: BaseSharedPreferencesTest() {

    @Test
    fun test_setAndGetInt() = testBlocking {
        val value = random.nextInt()
        prefernces.edit { putInt(TEST_KEY1, value) }
        assertEquals("Int values are not equal", value, prefernces.getIntValue(TEST_KEY1))
    }

    @Test
    fun test_setAndGetLong() = testBlocking {
        val value = random.nextLong()
        prefernces.edit { putLong(TEST_KEY1, value) }
        assertEquals("Long values are not equal", value, prefernces.getLongValue(TEST_KEY1))
    }

    @Test
    fun test_setAndGetFloat() = testBlocking {
        val value = random.nextFloat()
        prefernces.edit { putFloat(TEST_KEY1, value) }
        assertEquals("Float values are not equal", value, prefernces.getFloatValue(TEST_KEY1))
    }

    @Test
    fun test_setAndGetString() = testBlocking {
        val value = random.nextInt().toString()
        prefernces.edit { putString(TEST_KEY1, value) }
        assertEquals("String values are not equal", value, prefernces.getStringValue(TEST_KEY1))
    }

    @Test
    fun test_setAndGetStringSet() = testBlocking {
        val value: Set<String?> = HashSet<String>().apply {
            repeat(10) { add(random.nextInt().toString()) }
        }
        prefernces.edit { putStringSet(TEST_KEY1, value) }
        val restored = prefernces.getStringSetValue(TEST_KEY1)!!
        assertTrue("String set values are not equal",
            value.containsAll(restored) && restored.containsAll(value)
        )
    }

    @Test
    fun test_setAndGetBoolean() = testBlocking {
        val value = random.nextBoolean()
        prefernces.edit { putBoolean(TEST_KEY1, value) }
        assertEquals("Boolean values are not equal", value, prefernces.getBooleanValue(TEST_KEY1))
    }
}