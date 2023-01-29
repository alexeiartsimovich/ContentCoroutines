package android.content.coroutines

import androidx.annotation.AnyThread
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

object ContentCoroutines {
    private val ioExecutorRef = AtomicReference<Executor?>(null)
    private val defaultIoExecutor: Executor by lazy { createDefaultIoExecutor() }

    internal val ioExecutor: Executor get() = ioExecutorRef.get() ?: defaultIoExecutor

    @AnyThread
    @JvmStatic
    fun setIoExecutor(executor: Executor) {
        ioExecutorRef.set(executor)
    }

    private fun createDefaultIoExecutor(): Executor {
        val coreThreadNumber = 8
        val maxThreadNumber = 32
        val keepAliveTimeInSeconds = 10L
        val threadId = AtomicLong(0)
        val threadFactory = ThreadFactory { runnable ->
            val name = "ContentCoroutinesThread-${threadId.getAndIncrement()}"
            return@ThreadFactory Thread(runnable, name)
        }
        val rejectedExecutionHandler =
            RejectedExecutionHandler { r: Runnable, executor: ThreadPoolExecutor ->
                val exception = RejectedExecutionException("Task $r rejected from $executor")
                throw exception
            }
        // allow core thread timeout to timeout the core threads so that
        // when no tasks arrive, the core threads can be killed
        val executor = ThreadPoolExecutor(
            coreThreadNumber,
            maxThreadNumber,
            keepAliveTimeInSeconds,
            TimeUnit.SECONDS,
            LinkedBlockingDeque() /* unbounded queue */,
            threadFactory,
            rejectedExecutionHandler
        )
        executor.allowCoreThreadTimeOut(true)
        return executor
    }
}