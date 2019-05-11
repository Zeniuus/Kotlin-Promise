package dev.suhwan.kotlin.promise

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

suspend fun CoroutineScope.await(block: () -> Promise): Any {
    val promise = block()

    val lock = Mutex()
    val job = launch {
        lock.lock()
        promise.then({
            AwaitResultHolder.set(promise, it)
            lock.unlock()
        })
        lock.lock()
    }
    job.join()
    return AwaitResultHolder.get(promise)
}

private object AwaitResultHolder {
    private val map = mutableMapOf<Promise, Any>()

    fun get(promise: Promise): Any {
        val result = map[promise] ?: throw IllegalArgumentException("Should not reach here!")
        map.remove(promise)
        return result
    }

    fun set(promise: Promise, result: Any) {
        map[promise] = result
    }
}
