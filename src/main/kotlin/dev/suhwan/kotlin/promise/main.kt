package dev.suhwan.kotlin.promise

import kotlin.random.Random

fun main() {
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
