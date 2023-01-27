package android.content.coroutines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesCoroutinesTest: BaseSharedPreferencesTest() {

    @Test
    fun test_setAndGetInt() = testBlocking {
        val value = random.nextInt()
        prefernces.editCo { putInt(TEST_KEY1, value) }
        assertEquals("Int values are not equal", value, prefernces.getIntCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetLong() = testBlocking {
        val value = random.nextLong()
        prefernces.editCo { putLong(TEST_KEY1, value) }
        assertEquals("Long values are not equal", value, prefernces.getLongCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetFloat() = testBlocking {
        val value = random.nextFloat()
        prefernces.editCo { putFloat(TEST_KEY1, value) }
        assertEquals("Float values are not equal", value, prefernces.getFloatCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetString() = testBlocking {
        val value = random.nextInt().toString()
        prefernces.editCo { putString(TEST_KEY1, value) }
        assertEquals("String values are not equal", value, prefernces.getStringCo(TEST_KEY1))
    }

    @Test
    fun test_setAndGetStringSet() = testBlocking {
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
    fun test_setAndGetBoolean() = testBlocking {
        val value = random.nextBoolean()
        prefernces.editCo { putBoolean(TEST_KEY1, value) }
        assertEquals("Boolean values are not equal", value, prefernces.getBooleanCo(TEST_KEY1))
    }
}