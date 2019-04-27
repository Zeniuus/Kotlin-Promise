package dev.suhwan.kotlin.promise

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

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

fun main() {
    basicUsage()
    withAsyncJobs()
}

fun runBlockingWithinScope(block: (CoroutineScope) -> Any) {
    runBlocking {
        val prevCoroutineScope = CoroutineScopeHolder.getCoroutineScope()
        CoroutineScopeHolder.setCoroutineScope(this)
        val result = block(this)
        CoroutineScopeHolder.setCoroutineScope(prevCoroutineScope)
        result
    }
}
