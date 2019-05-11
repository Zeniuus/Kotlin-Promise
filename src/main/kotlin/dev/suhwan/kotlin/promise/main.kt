package dev.suhwan.kotlin.promise

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlinx.coroutines.launch

fun basicUsage() = runBlockingWithinScope {
    val random = Random.nextBoolean()
    val promise = Promise { resolve, reject ->
        if (random) {
            println("Success!")
            resolve("success")
        } else {
            println("Failure...")
            reject("failure")
        }
    }

    promise
        .then({ res ->
            println("Just got successful result: $res; but now throw an AssertionError")
            throw AssertionError("Some assertion failed")
        }, { err ->
            println("Just got failed result: $err; now return a successful value")
            "now successful"
        })
        .then({ res ->
            println("Just got successful result: $res; in this then(), there is no handler for rejected Promise.")
        })
        .catch { err ->
            println("Final all-exception-eating handler, just got: $err")
        }
}

fun withAsyncJobs() = runBlockingWithinScope {
    suspend fun someAsyncJob(): String {
        delay(2000)
        return "some result"
    }

    suspend fun someFailingAsyncJob(): String {
        delay(1000)
        throw IOException("some error")
    }

    val promiseWithAsyncJob = Promise { resolve, reject ->
        val result = someAsyncJob()
        println("async job finished: $result")
        resolve(result)
    }
    promiseWithAsyncJob.then({
        println("result: $it")
    })

    val promiseWithFailingAsyncJob = Promise { resolve, reject ->
        val result = someFailingAsyncJob()
        println("async job finished: $result")
        resolve(result)
    }
    promiseWithFailingAsyncJob.then({
        println("result: $it")
    }, {
        println("job failed: $it")
    })
}

fun promiseReturnsPromise() = runBlockingWithinScope {
    suspend fun someAsyncJob(): String {
        delay(2000)
        return "some result"
    }

    suspend fun someFailingAsyncJob(): String {
        delay(1000)
        throw IOException("some error")
    }

    val getPromiseWithAsyncJob = getPromiseGenerator(::someAsyncJob)
    val getPromiseWithFailingAsyncJob = getPromiseGenerator(::someFailingAsyncJob)

    getPromiseWithAsyncJob()
        .then({
            println("result 1: $it")
            getPromiseWithAsyncJob()
        })
        .then({
            println("result 2: $it")
            getPromiseWithAsyncJob()
        })
        .then({
            println("result 3: $it")
            getPromiseWithFailingAsyncJob()
        })
        .then({
            println("Should not reach here")
        }, {
            println("Gotcha, $it!")
        })
}

fun awaitUsage() = runBlockingWithinScope {
    suspend fun someAsyncJob(): String {
        delay(2000)
        return "some result"
    }

    val doAsyncJob = getPromiseGenerator(::someAsyncJob)

    it.launch {
        println("Let's test await syntax!")
        val result = await { doAsyncJob() }
        println("result: $result")
        await { doAsyncJob() }
        println("result: $result")
        await { doAsyncJob() }
        println("result: $result")
        println("How was it?")
    }
}

fun main() {
    basicUsage()
    withAsyncJobs()
    promiseReturnsPromise()
    awaitUsage()
}

fun runBlockingWithinScope(block: (CoroutineScope) -> Any) {
    runBlocking {
        val prevCoroutineScope = CoroutineScopeHolder.getCoroutineScope()
        CoroutineScopeHolder.setCoroutineScope(this)
        val result = block(this)
//        CoroutineScopeHolder.setCoroutineScope(prevCoroutineScope)
        result
    }
}

fun getPromiseGenerator(block: suspend () -> Any): () -> Promise {
    return {
        Promise { resolve, reject ->
            val result = block()
            println("async job finished: $result")
            resolve(result)
        }
    }
}
