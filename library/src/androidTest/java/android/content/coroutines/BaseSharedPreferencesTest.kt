package android.content.coroutines

import android.content.Context
import android.content.SharedPreferences
import androidx.test.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import kotlin.random.Random

abstract class BaseSharedPreferencesTest {
    protected lateinit var prefernces: SharedPreferences
        private set
    protected lateinit var random: Random
        private set

    @Before
    fun setUp() {
        prefernces = InstrumentationRegistry.getContext()
            .getSharedPreferences("test", Context.MODE_PRIVATE)
        random = Random(System.currentTimeMillis())
    }

    @After
    fun tearDown() {
        prefernces.edit().clear().commit()
    }

    protected fun testBlocking(block: suspend CoroutineScope.() -> Unit) {
        runBlocking(block = block)
    }

    companion object {
        @JvmStatic
        protected val TEST_KEY1 = "test_key1"
    }
}