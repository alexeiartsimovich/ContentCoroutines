package android.content.coroutines

import java.util.concurrent.Executor
import java.util.concurrent.Executors


internal object ExecutorProvider {
    val io: Executor by lazy {
        Executors.newFixedThreadPool(2)
    }
}